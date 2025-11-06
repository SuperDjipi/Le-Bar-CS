package club.djipi.lebarcs.ui.screens.game

import androidx.lifecycle.ViewModel
import club.djipi.lebarcs.domain.model.PlacedTile
import club.djipi.lebarcs.domain.model.Player
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class GameViewModel @Inject constructor(
    // TODO: Injecter le repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadMockGameData()
    }

    private fun loadMockGameData() {
        val mockPlayers = listOf(
            Player("1", "Vous", 42, listOf(
                Tile('S', 1),
                Tile('C', 3),
                Tile('R', 1),
                Tile('A', 1),
                Tile('B', 3),
                Tile('L', 1),
                Tile('E', 1)
            )),
            Player("2", "Adversaire", 38, emptyList())
        )

        _uiState.value = GameUiState.Playing(
            GameData(
                players = mockPlayers,
                board = Board(),
                currentPlayerIndex = 0,
                currentPlayerRack = mockPlayers[0].rack,
                placedTiles = emptyList()
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

            println("✅ Tuile '${tile.letter}' placée à la position ${position.row},${position.col}")

            // Ajouter la tuile aux tuiles placées
            val newPlacedTile = PlacedTile(tile, position)
            val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile

            // Mettre à jour le plateau
            val newBoard = updateBoardWithTileFromRack(currentState.gameData.board, tile, position)

            // Retirer la tuile du chevalet
            val newRack = currentState.gameData.currentPlayerRack.toMutableList().apply {
                removeAt(rackIndex)
            }

            _uiState.update {
                currentState.copy(
                    gameData = currentState.gameData.copy(
                        board = newBoard,
                        currentPlayerRack = newRack,
                        placedTiles = updatedPlacedTiles
                    )
                )
            }
        }
    }

    fun onTileMovedOnBoard(fromPosition: Position, toPosition: Position) {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            println("✅ Tuile déplacée de $fromPosition à $toPosition")

            // Logique à implémenter :
            // 1. Trouver la tuile à la position 'fromPosition' sur le plateau.
            // 2. Vérifier que 'toPosition' est libre.
            // 3. Mettre à jour le plateau : retirer la tuile de 'from' et la mettre à 'to'.
            val newBoard = updateBoardWithTileFromBoard(currentState.gameData.board, fromPosition, toPosition)
            // 4. Emettre le nouvel état.
            _uiState.update {
                currentState.copy(
                    gameData = currentState.gameData.copy(
                        board = newBoard
                    )
                )
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

    private fun updateBoardWithTileFromBoard(board: Board, fromPosition: Position, toPosition: Position): Board {
        val newCells = board.cells.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, cell ->
                if (rowIndex == fromPosition.row && colIndex == fromPosition.col) {
                    cell.copy(tile = null, isLocked = false)
                } else if (rowIndex == toPosition.row && colIndex == toPosition.col) {
                    cell.copy(tile = board.cells[fromPosition.row][fromPosition.col].tile, isLocked = false)
                } else {
                    cell
                }
            }
        }
        return Board(newCells)
    }

    fun onShuffleRack() {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            val shuffledRack = currentState.gameData.currentPlayerRack.shuffled()
            _uiState.update {
                currentState.copy(
                    gameData = currentState.gameData.copy(
                        currentPlayerRack = shuffledRack
                    )
                )
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
