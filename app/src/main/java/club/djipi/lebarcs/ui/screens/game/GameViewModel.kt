package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.activity.result.launch
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
import club.djipi.lebarcs.shared.generateUUID
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GameViewModel"

@HiltViewModel
class GameViewModel @Inject constructor(
    private val dictionary: Dictionary,
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadMockGameData()
    }

    private fun loadMockGameData() {
        val mockPlayers = listOf(
            Player(
                "1", "Vous", 42, listOf(
                    Tile(letter = 'S', points = 1),
                    Tile(letter = 'C', points = 3),
                    Tile(letter = 'R', points = 1),
                    Tile(letter = 'A', points = 1),
                    Tile(letter = 'B', points = 3),
                    Tile(letter = 'L', points = 1),
                    Tile(letter = 'E', points = 1)
                )
            ),
            Player("2", "Adversaire", 38, emptyList())
        )

        _uiState.value = GameUiState.Playing(
            GameState(
                id = "123",
                players = mockPlayers,
                board = Board(),
                currentPlayerIndex = 0,
                currentPlayerRack = mockPlayers[0].rack,
            )
        )
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

    // NOUVELLE FONCTION pour Board -> Rack
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

    // NOUVELLE FONCTION pour Rack -> Rack
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
        newRack: List<Tile> // On ajoute le nouveau rack en paramètre
    ) {
        val placedTilesMap = newPlacedTiles.associate { it.position to it.tile }
        val originalBoard = currentState.gameData.board // On a besoin du plateau d'origine
        // Si aucune tuile n'est posée, le coup est "vide" mais valide pour continuer.
        if (newPlacedTiles.isEmpty()) {
            _uiState.update {
                currentState.copy(
                    gameData = currentState.gameData.copy(
                        board = newBoard,
                        currentPlayerRack = newRack,
                        placedTiles = emptyList(),
                        foundWords = emptyList(),
                        currentMoveScore = 0,
                        isCurrentMoveValid = false // Un coup vide n'est pas jouable
                    )
                )
            }
            return
        }    // 1. Validation de l'alignement et de la contiguïté
        val isPlacementValid = MoveValidator.isPlacementValid(originalBoard, placedTilesMap.keys)

        // 2. Validation des mots dans le dictionnaire
        val foundWords = WordFinder.findAllWordsFormedByMove(newBoard, placedTilesMap)
        val allWordsAreInDico =
            foundWords.isNotEmpty() && foundWords.all { dictionary.isValid(it.text) }

        // 3. Validation de la connexion au jeu existant
        val isMoveConnected = MoveValidator.isMoveConnected(
            board = originalBoard,
            placedTiles = placedTilesMap.keys,
            turnNumber = currentState.gameData.turnNumber // On utilise gameState
        )

        // Le coup est JOUABLE si TOUTES les conditions sont remplies
        val isMovePlayable = isPlacementValid && allWordsAreInDico && isMoveConnected

        var totalScore = 0
        // On calcule le score seulement si le coup est potentiellement jouable.
        if (isMovePlayable) {
            foundWords.forEach { word ->
                totalScore += ScoreCalculator.calculateScore(
                    word,
                    newBoard,
                    placedTilesMap.keys.toList()
                )
            }
            if (placedTilesMap.size == 7) {
                totalScore += 50
            }
        }
        _uiState.update {
            currentState.copy(
                gameData = currentState.gameData.copy(
                    board = newBoard,
                    currentPlayerRack = newRack,
                    placedTiles = newPlacedTiles,
                    foundWords = foundWords.toList(),
                    currentMoveScore = totalScore,
                    isPlacementValid = isPlacementValid,
                    areWordsValid = allWordsAreInDico,
                    isCurrentMoveValid = isMovePlayable
                )
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

    fun onPlayMove() {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing && currentState.gameData.isCurrentMoveValid) {

                val gameData = currentState.gameData
                println("✅ Coup valide ! On joue le coup pour ${gameData.currentMoveScore} points.")

                // --- DÉBUT DE LA LOGIQUE DE FIN DE TOUR ---

                // 1. Mettre à jour le score du joueur actuel
                val updatedPlayers = gameData.players.mapIndexed { index, player ->
                    if (index == gameData.currentPlayerIndex) {
                        player.copy(score = player.score + gameData.currentMoveScore)
                    } else {
                        player
                    }
                }

                // 2. "Verrouiller" les tuiles. Le 'newBoard' est déjà correct car
                //    il a été calculé par 'updateStateWithNewMove'. Les tuiles sont déjà en place.
                //    On n'a rien de plus à faire sur le plateau lui-même.

                // 3. Faire piocher de nouvelles tuiles
                //    (Pour l'instant, la pioche est vide, mais on prépare la logique)
                val tilesToDraw = 7 - gameData.currentPlayerRack.size
                // val newTilesFromBag = tileBag.draw(tilesToDraw) // Logique future
                // val newRackAfterDraw = gameData.currentPlayerRack + newTilesFromBag
                // Pour l'instant, on garde le chevalet tel qu'il est après avoir posé les tuiles.
                val finalRack = gameData.currentPlayerRack

                // 4. Passer au joueur suivant
                val nextPlayerIndex = (gameData.currentPlayerIndex + 1) % gameData.players.size

                // 5. Préparer l'état pour le prochain tour
                currentState.copy(
                    gameData = gameData.copy(
                        // L'état du jeu est mis à jour
                        players = updatedPlayers,
                        // Le plateau est déjà le bon
                        turnNumber = gameData.turnNumber + 1,
                        currentPlayerIndex = nextPlayerIndex,
                        currentPlayerRack = finalRack, // Mettre à jour le chevalet avec les nouvelles tuiles piochées

                        // On réinitialise l'état du coup en cours
                        placedTiles = emptyList(),
                        foundWords = emptyList(),
                        currentMoveScore = 0,
                        isPlacementValid = true,
                        areWordsValid = true,
                        isMoveConnected = true, // La connexion n'est pas pertinente pour un coup "vide"
                        isCurrentMoveValid = false // Le prochain coup n'est pas encore valide
                    )
                )
                // --- FIN DE LA LOGIQUE DE FIN DE TOUR ---

            } else {
                println("❌ Coup invalide, impossible de jouer.")
                // Retourne l'état actuel sans changement si le coup n'est pas valide
                currentState
            }
        }
    }

    fun onPass() {
        println("Passer le tour")
        // TODO: Passer le tour
    }


    fun connectToGame(gameId: String) {
        viewModelScope.launch {
            try {
                // On se connecte et on écoute le flux d'événements du serveur
                webSocketClient.connect(gameId).collect { event ->
                    handleServerEvent(event)
                }
            } catch (e: Exception) {
                _uiState.value = GameUiState.Error("Impossible de se connecter au serveur.")
            }
        }
    }

    private fun handleServerEvent(event: ServerToClientEvent) {
        when (event) {
            // Le serveur nous envoie le nouvel état du jeu !
            is ServerToClientEvent.GameStateUpdate -> {
                // On met simplement à jour notre UI avec l'état officiel reçu.
                _uiState.update {
                    if (it is GameUiState.Playing) {
                        it.copy(gameData = event.payload.gameState)
                    } else {
                        // C'est la première fois qu'on reçoit l'état, on passe à l'état Playing
                        GameUiState.Playing(gameData = event.payload.gameState)
                    }
                }
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
        webSocketClient.close() // Très important de fermer la connexion
    }


}