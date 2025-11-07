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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import club.djipi.lebarcs.domain.model.*
import club.djipi.lebarcs.shared.domain.model.*
import club.djipi.lebarcs.ui.screens.game.components.GameContent
import club.djipi.lebarcs.ui.screens.game.components.TileView
import club.djipi.lebarcs.ui.screens.game.dragdrop.ProvideDragDropManager
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

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
//                        },
//                        actions = {
//                            IconButton(onClick = { viewModel.onShuffleRack() }) {
//                                Icon(Icons.Default.Refresh, "Mélanger")
//                            }
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
                            onTilePlacedFromRack = viewModel::onTilePlacedFromRack,
                            onTileMovedOnBoard = viewModel::onTileMovedOnBoard,
                            onTileReturnedToRack = viewModel::onTileReturnedToRack,
                            onRackTilesReordered = viewModel::onRackTilesReordered,
                            onPlayMove = viewModel::onPlayMove,
                            onPass = viewModel::onPass,
                            onUndoMove = viewModel::onUndoMove,
                            onShuffleRack = viewModel::onShuffleRack,
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
            // --- L'OVERLAY EST ICI, AU NIVEAU RACINE ---
            // Il n'est PAS affecté par les paddingValues du Scaffold.
            if (dragDropManager.state.isDragging && dragDropManager.state.draggedTile != null) {
                val state = dragDropManager.state
                val tileSize = 60.dp
                val tileSizePx =
                    with(LocalDensity.current) { tileSize.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxSize() // Prend tout l'écran
                        .offset {
                            // On utilise les coordonnées globales directement
                            IntOffset(
                                x = (state.currentCoordinates.x - tileSizePx / 2).toInt(),
                                y = (state.currentCoordinates.y - tileSizePx / 2).toInt()
                            )
                        }
                ) {
                    TileView(
                        tile = state.draggedTile!!,
                        size = tileSize,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 1.3f
                            scaleY = 1.3f
                            shadowElevation = 16f
                            alpha = 0.7f
                        }
                    )
                }
            }

    }
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
                        Tile(letter = 'H', points = 4),
                        Tile(letter = 'E', points = 1),
                        Tile(letter = 'L', points = 1),
                        Tile(letter = 'L', points = 1),
                        Tile(letter = 'O', points = 1)
                    ),
                ),
                selectedTileIndex = null,
                dragDropManager = dragDropManager,
                onTileSelected = {},
                onCellClick = {},
                onTilePlacedFromRack = { _, _ -> },
                onTileMovedOnBoard = { _, _ -> },
                onTileReturnedToRack = {},
                onRackTilesReordered = { _, _ -> },
                onPlayMove = {},
                onPass = {},
                onShuffleRack = {},
                onUndoMove = {}
            )
        }
    }
}