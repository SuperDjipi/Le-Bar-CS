package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.ui.graphics.colorspace.connect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.domain.model.Player
import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.shared.domain.logic.MoveValidator
import club.djipi.lebarcs.shared.domain.logic.ScoreCalculator
import club.djipi.lebarcs.shared.domain.logic.WordFinder
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.GameState
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.domain.usecase.CalculateScoreUseCase
import club.djipi.lebarcs.shared.domain.usecase.ValidateMoveUseCase
import club.djipi.lebarcs.shared.generateUUID
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GameViewModel"

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val validateMoveUseCase: ValidateMoveUseCase,
    private val calculateScoreUseCase: CalculateScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    init {  // On lance une coroutine DÈS LA CRÉATION du ViewModel
        //  Son seul rôle est d'écouter les événements en permanence.
        Log.d(TAG, "Création du ViewModel (init)")
        viewModelScope.launch {
            gameRepository.getEvents().collect { event ->
                handleServerEvent(event)
            }
        }
    }
    fun onTileSelected(index: Int) {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            val newIndex = if (currentState.selectedTileIndex == index) null else index
            _uiState.update { currentState.copy(selectedTileIndex = newIndex) }
        }
    }

    fun onCellClick(position: Position) {
        println("Clicked on cell: $position")
    }

    fun onTilePlacedFromRack(rackIndex: Int, position: Position) {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            val tile = currentState.gameData.currentPlayerRack.getOrNull(rackIndex) ?: return
            // 1. On prépare les données pour la mise à jour de l'état
            val newPlacedTile = PlacedTile(tile, position)
            val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile
            val newRack = currentState.gameData.currentPlayerRack.toMutableList()
                .apply { removeAt(rackIndex) }
            val newBoard = updateBoardWithTileFromRack(currentState.gameData.board, tile, position)
            updateStateWithNewMove(currentState, newBoard, updatedPlacedTiles, newRack)
        }
    }

    fun onTileMovedOnBoard(fromPosition: Position, toPosition: Position) {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            // Le déplacement d'une tuile ne change ni le rack, ni les tuiles posées dans le tour.
            val updatedPlacedTiles = currentState.gameData.placedTiles.map {
                if (it.position == fromPosition) {
                    it.copy(position = toPosition) // On met juste à jour la position de la tuile déplacée
                } else {
                    it
                }
            }
            val newBoard =
                updateBoardWithTileFromBoard(currentState.gameData.board, fromPosition, toPosition)
            updateStateWithNewMove(
                currentState,
                newBoard,
                updatedPlacedTiles,
                currentState.gameData.currentPlayerRack
            )
        }
    }

    fun onTileReturnedToRack(fromPosition: Position) {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                val board = currentState.gameData.board
                val tileToReturn = board.cells[fromPosition.row][fromPosition.col].tile
                    ?: return@update currentState

                // 1. Crée un nouveau plateau sans la tuile
                val newBoard =
                    updateBoardWithTileFromBoard(board, fromPosition, null) // 'null' pour effacer

                // 2. Crée un nouveau chevalet avec la tuile revenue
                val newRack = currentState.gameData.currentPlayerRack + tileToReturn

                // 3. Appelle la fonction de mise à jour commune
                updateStateWithNewMove(
                    currentState = currentState,
                    newBoard = newBoard,
                    newPlacedTiles = currentState.gameData.placedTiles.filter { it.position != fromPosition },
                    newRack = newRack
                )
                currentState // Retourne l'état mis à jour via l'helper
            } else {
                currentState
            }
        }
    }

    fun onRackTilesReordered(fromIndex: Int, toIndex: Int) {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                val currentRack = currentState.gameData.currentPlayerRack.toMutableList()
                if (fromIndex < 0 || fromIndex >= currentRack.size || toIndex < 0 || toIndex > currentRack.size) {
                    return@update currentState // Sécurité pour éviter les crashs
                }

                // Réorganise la liste
                val draggedTile = currentRack.removeAt(fromIndex)
                currentRack.add(toIndex.coerceAtMost(currentRack.size), draggedTile)

                // Pas besoin d'appeler l'helper de calcul de score, on met juste à jour le chevalet
                currentState.copy(
                    gameData = currentState.gameData.copy(currentPlayerRack = currentRack)
                )
            } else {
                currentState
            }
        }
    }

    private fun updateBoardWithTileFromRack(board: Board, tile: Tile, position: Position): Board {
        val newCells = board.cells.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, cell ->
                if (rowIndex == position.row && colIndex == position.col) {
                    cell.copy(tile = tile, isLocked = false)
                } else {
                    cell
                }
            }
        }
        return Board(newCells)
    }

    private fun updateBoardWithTileFromBoard(
        board: Board,
        fromPosition: Position,
        toPosition: Position?
    ): Board {
        val newCells = board.cells.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, cell ->
                if (rowIndex == fromPosition.row && colIndex == fromPosition.col) {
                    cell.copy(tile = null, isLocked = false)
                } else if (rowIndex == toPosition?.row && colIndex == toPosition.col) {
                    cell.copy(
                        tile = board.cells[fromPosition.row][fromPosition.col].tile,
                        isLocked = false
                    )
                } else {
                    cell
                }
            }
        }
        return Board(newCells)
    }

    private fun updateStateWithNewMove(
        currentState: GameUiState.Playing,
        newBoard: Board,
        newPlacedTiles: List<PlacedTile>,
        newRack: List<Tile>
    ) {
        // Le ViewModel utilise les UseCases pour la logique métier complexe
        val isMovePlayable = validateMoveUseCase(
            originalBoard = currentState.gameData.board,
            newBoard = newBoard,
            placedTiles = newPlacedTiles,
            turnNumber = currentState.gameData.turnNumber
        )

        // On recalcule les mots trouvés et le score
        val foundWords = if (isMovePlayable) {
            WordFinder.findAllWordsFormedByMove(newBoard, newPlacedTiles.associate { it.position to it.tile })
        } else {
            emptySet()
        }

        val score = if (isMovePlayable) {
            calculateScoreUseCase(foundWords, newBoard, newPlacedTiles.map { it.position })
        } else {
            0
        }

        _uiState.update {
            // 'it' est le 'currentState' (GameUiState.Playing)
            (it as GameUiState.Playing).copy(
                // On met à jour l'état permanent du jeu avec les nouvelles tuiles posées "visuellement"
                gameData = it.gameData.copy(
                    board = newBoard,
                    currentPlayerRack = newRack,
                    placedTiles = newPlacedTiles // 'placedTiles' est bien une propriété du GameState partagé
                ),

                // On met à jour les informations TEMPORAIRES sur le coup en cours
                foundWordsForCurrentMove = foundWords.toList(),
                currentMoveScore = score,
                isCurrentMoveValid = isMovePlayable
            )
        }
    }

    fun onShuffleRack() {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            val shuffledRack = currentState.gameData.currentPlayerRack.shuffled()
            _uiState.update {
                currentState.copy(
                    gameData = currentState.gameData.copy(currentPlayerRack = shuffledRack)
                )
            }
        }
    }

    /** Annule le coup en cours en remettant toutes les tuiles posées
     * sur le plateau dans le chevalet du joueur.
     */
    fun onUndoMove() {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                val gameData = currentState.gameData
                if (gameData.placedTiles.isEmpty()) {
                    // S'il n'y a aucune tuile à annuler, on ne fait rien.
                    return@update currentState
                }

                // 1. On récupère les tuiles qui avaient été posées
                val tilesToReturn = gameData.placedTiles.map { it.tile }

                // 2. On recrée un chevalet avec les tuiles du chevalet actuel ET les tuiles retournées
                val newRack = gameData.currentPlayerRack + tilesToReturn

                // 3. On recrée un plateau propre, sans les tuiles qui venaient d'être posées.
                // Pour cela, on part du plateau *d'origine* (avant le début du coup).
                // Si on ne l'a pas, on peut le reconstruire en retirant les 'placedTiles'.
                var newBoard = gameData.board
                gameData.placedTiles.forEach { placedTile ->
                    newBoard = updateBoardWithTileFromBoard(
                        newBoard,
                        placedTile.position,
                        null
                    ) // 'null' pour effacer
                }

                // 4. On réinitialise complètement l'état du coup en cours
                currentState.copy(
                    gameData = gameData.copy(
                        board = newBoard,
                        currentPlayerRack = newRack,
                        placedTiles = emptyList(), // Le coup en cours est vide
                        foundWords = emptyList(),
                        currentMoveScore = 0,
                        isCurrentMoveValid = false
                    )
                )
            } else {
                currentState
            }
        }
    }

    // --- Actions de l'utilisateur ---
    fun onPlayMove() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.isCurrentMoveValid) {
                // Le ViewModel délègue l'envoi du coup au Repository
                gameRepository.sendPlayMove(currentState.gameData.placedTiles)
            }
        }
    }

    fun onPass() {
        println("Passer le tour")
        // TODO: Passer le tour
    }

    // --- Fonctions de communication ---
    fun connectToGame(gameId: String) {
            // Cette fonction ne fait plus qu'une seule chose : déclencher.
            // Elle n'est plus 'suspend', elle ne bloque pas.
            _uiState.value = GameUiState.Loading
            gameRepository.connect(gameId)
    }

    private fun handleServerEvent(event: ServerToClientEvent) {
        Log.e("GameViewModel", "Event reçu : $event")
        when (event) {

            // Le serveur nous envoie le nouvel état du jeu !
            is ServerToClientEvent.GameStateUpdate -> {
                Log.d("GameViewModel", "Nouvel état du jeu reçu : ${event.payload.gameState}")
                _uiState.value = GameUiState.Playing(gameData = event.payload.gameState)
            }

            is ServerToClientEvent.ErrorMessage -> {
                // Le serveur nous dit qu'on a fait une erreur
                Log.e("GameViewModel", "Erreur du serveur : ${event.payload.message}")
                // On pourrait afficher un Toast ou un Snackbar
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameRepository.close() // Très important de fermer la connexion
    }


}