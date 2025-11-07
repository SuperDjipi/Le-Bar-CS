package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import club.djipi.lebarcs.domain.model.PlacedTile
import club.djipi.lebarcs.domain.model.Player
import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.shared.domain.logic.MoveValidator
import club.djipi.lebarcs.shared.domain.logic.ScoreCalculator
import club.djipi.lebarcs.shared.domain.logic.WordFinder
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "GameViewModel"

@HiltViewModel
class GameViewModel @Inject constructor(
    private val dictionary: Dictionary
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadMockGameData()
    }

    private fun loadMockGameData() {
        val mockPlayers = listOf(
            Player("1", "Vous", 42, listOf(
                Tile(letter = 'S', points = 1),
                Tile(letter = 'C', points = 3),
                Tile(letter = 'R', points = 1),
                Tile(letter = 'A', points = 1),
                Tile(letter = 'B', points = 3),
                Tile(letter = 'L', points = 1),
                Tile(letter = 'E', points = 1)
            )),
            Player("2", "Adversaire", 38, emptyList())
        )

        _uiState.value = GameUiState.Playing(
            GameData(
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
            val newRack = currentState.gameData.currentPlayerRack.toMutableList().apply { removeAt(rackIndex) }
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
            val newBoard = updateBoardWithTileFromBoard(currentState.gameData.board, fromPosition, toPosition)
            updateStateWithNewMove(currentState, newBoard, updatedPlacedTiles, currentState.gameData.currentPlayerRack)
        }
    }

    // NOUVELLE FONCTION pour Board -> Rack
    fun onTileReturnedToRack(fromPosition: Position) {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                val board = currentState.gameData.board
                val tileToReturn = board.cells[fromPosition.row][fromPosition.col].tile ?: return@update currentState

                // 1. Crée un nouveau plateau sans la tuile
                val newBoard = updateBoardWithTileFromBoard(board, fromPosition, null) // 'null' pour effacer

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
                    gameData = currentState.gameData.copy(currentPlayerRack = currentRack,)
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

    private fun updateBoardWithTileFromBoard(board: Board, fromPosition: Position, toPosition: Position?): Board {
        val newCells = board.cells.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, cell ->
                if (rowIndex == fromPosition.row && colIndex == fromPosition.col) {
                    cell.copy(tile = null, isLocked = false)
                } else if (rowIndex == toPosition?.row && colIndex == toPosition.col) {
                    cell.copy(tile = board.cells[fromPosition.row][fromPosition.col].tile, isLocked = false)
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
                )}
            return
        }
        val isPlacementValid = MoveValidator.isPlacementValid(placedTilesMap.keys)
        val foundWords = WordFinder.findAllWordsFormedByMove(newBoard, placedTilesMap)
        val allWordsAreInDico = foundWords.isNotEmpty() && foundWords.all { dictionary.isValid(it.text) }

        val isMovePlayable = isPlacementValid && allWordsAreInDico
        var totalScore = 0
        // On calcule le score seulement si le coup est potentiellement jouable.
        if (isMovePlayable) {
            foundWords.forEach{ word ->
                totalScore += ScoreCalculator.calculateScore(word, newBoard, placedTilesMap.keys.toList())
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
                    newBoard = updateBoardWithTileFromBoard(newBoard, placedTile.position, null) // 'null' pour effacer
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
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            println("Jouer le coup avec ${currentState.gameData.placedTiles.size} tuile(s)")
            // TODO: Envoyer le coup au serveur
            // TODO: Valider le coup
            // TODO: Calculer le score
        }
    }

    fun onPass() {
        println("Passer le tour")
        // TODO: Passer le tour
    }
}
