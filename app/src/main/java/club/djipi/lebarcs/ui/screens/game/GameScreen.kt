package club.djipi.lebarcs.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.domain.model.*
import club.djipi.lebarcs.shared.domain.model.*
import club.djipi.lebarcs.ui.screens.game.components.BoardView
import club.djipi.lebarcs.ui.screens.game.components.ScoreBoard
import club.djipi.lebarcs.ui.screens.game.components.TileRack
import club.djipi.lebarcs.ui.screens.game.components.TileView
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.ProvideDragDropManager
import club.djipi.lebarcs.ui.theme.LeBarCSTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameId: String,
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ProvideDragDropManager { dragDropManager ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Partie en cours") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onShuffleRack() }) {
                            Icon(Icons.Default.Refresh, "Mélanger")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is GameUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GameUiState.Playing -> {
                    GameContent(
                        gameData = state.gameData,
                        selectedTileIndex = state.selectedTileIndex,
                        dragDropManager = dragDropManager,
                        onTileSelected = viewModel::onTileSelected,
                        onCellClick = viewModel::onCellClick,
                        onTilePlaced = viewModel::onTilePlaced,
                        onPlayMove = viewModel::onPlayMove,
                        onPass = viewModel::onPass,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                is GameUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Erreur: ${state.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = onNavigateBack) {
                                Text("Retour")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameContent(
    gameData: GameData,
    selectedTileIndex: Int?,
    dragDropManager: DragDropManager,
    onTileSelected: (Int) -> Unit,
    onCellClick: (Position) -> Unit,
    onTilePlaced: (Int, Position) -> Unit,
    onPlayMove: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observer les résultats du drag & drop
    LaunchedEffect(dragDropManager.state) {
        val state = dragDropManager.state
        if (!state.isDragging && state.targetPosition != null) {
            state.draggedFromRackIndex?.let { rackIndex ->
                state.targetPosition.let { position ->
                    onTilePlaced(rackIndex, position)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Contenu principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Scores
            ScoreBoard(
                players = gameData.players,
                currentPlayerIndex = gameData.currentPlayerIndex,
                modifier = Modifier.fillMaxWidth()
            )

            // Plateau avec drag & drop
            BoardView(
                board = gameData.board,
                cellSize = 30,
                dragDropManager = dragDropManager,
                onCellClick = onCellClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Chevalet avec drag & drop
            TileRack(
                tiles = gameData.currentPlayerRack,
                selectedIndex = selectedTileIndex,
                dragDropManager = dragDropManager,
                onTileClick = onTileSelected,
                onTileDragStart = { index ->
                    println("Début du drag de la tuile à l'index $index")
                },
                onTileDragEnd = { index ->
                    println("Fin du drag de la tuile à l'index $index")
                }
            )

            // Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPass,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Passer")
                }

                Button(
                    onClick = onPlayMove,
                    modifier = Modifier.weight(1f),
                    enabled = gameData.hasPlacedTiles
                ) {
                    Text("Jouer")
                }
            }
        }

        // Overlay pour la tuile draggée (au-dessus de tout)
        if (dragDropManager.state.isDragging && dragDropManager.state.draggedTile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            dragDropManager.state.dragOffset.x.toInt(),
                            dragDropManager.state.dragOffset.y.toInt()
                        )
                    }
            ) {
                TileView(
                    tile = dragDropManager.state.draggedTile!!,
                    size = 60.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.3f
                        scaleY = 1.3f
                        shadowElevation = 16f
                        alpha = 0.9f
                    }
                )
            }
        }
    }
}

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

    fun onTilePlaced(rackIndex: Int, position: Position) {
        val currentState = _uiState.value
        if (currentState is GameUiState.Playing) {
            val tile = currentState.gameData.currentPlayerRack.getOrNull(rackIndex) ?: return

            println("✅ Tuile '${tile.letter}' placée à la position ${position.row},${position.col}")

            // Ajouter la tuile aux tuiles placées
            val newPlacedTile = PlacedTile(tile, position)
            val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile

            // Mettre à jour le plateau
            val newBoard = updateBoardWithTile(currentState.gameData.board, tile, position)

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

    private fun updateBoardWithTile(board: Board, tile: Tile, position: Position): Board {
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

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val gameData: GameData,
        val selectedTileIndex: Int? = null
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}

data class GameData(
    val players: List<Player>,
    val board: Board,
    val currentPlayerIndex: Int,
    val currentPlayerRack: List<Tile>,
    val placedTiles: List<PlacedTile> = emptyList()
) {
    val hasPlacedTiles: Boolean
        get() = placedTiles.isNotEmpty()
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    LeBarCSTheme {
        ProvideDragDropManager { dragDropManager ->
            GameContent(
                gameData = GameData(
                    players = listOf(
                        Player("1", "Alice", 125, emptyList()),
                        Player("2", "Bob", 98, emptyList())
                    ),
                    board = Board(),
                    currentPlayerIndex = 0,
                    currentPlayerRack = listOf(
                        Tile('H', 4),
                        Tile('E', 1),
                        Tile('L', 1),
                        Tile('L', 1),
                        Tile('O', 1)
                    )
                ),
                selectedTileIndex = null,
                dragDropManager = dragDropManager,
                onTileSelected = {},
                onCellClick = {},
                onTilePlaced = { _, _ -> },
                onPlayMove = {},
                onPass = {}
            )
        }
    }
}