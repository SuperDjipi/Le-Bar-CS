package club.djipi.lebarcs.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import club.djipi.lebarcs.shared.domain.model.GameStatus
import club.djipi.lebarcs.ui.screens.game.components.GameContent
import club.djipi.lebarcs.ui.screens.game.components.EndGameDialog
import club.djipi.lebarcs.ui.screens.game.components.JokerSelectionDialog
import club.djipi.lebarcs.ui.screens.game.components.TileView
import club.djipi.lebarcs.ui.screens.game.dragdrop.ProvideDragDropManager
import kotlin.text.toInt

/**
 * Le `Composable` principal pour l'écran de jeu.
 *
 * C'est un composant "orchestrateur" dont le rôle est d'assembler les différentes
 * parties de l'UI du jeu. Il est responsable de :
 * 1.  Afficher la structure globale de l'écran (la barre de titre) via un `Scaffold`.
 * 2.  Observer l'état (`GameUiState`) depuis le `GameViewModel` partagé.
 * 3.  Afficher conditionnellement le bon contenu en fonction de l'état :
 *     - Écran de chargement (`Loading`).
 *     - Contenu du jeu (`Playing`).
 *     - Message d'erreur (`Error`).
 * 4.  Gérer les éléments qui se superposent à toute l'UI, comme la "tuile flottante"
 *     lors d'un Drag & Drop, et les boîtes de dialogue (comme celle du joker).
 *
 * @param onNavigateBack Une fonction de rappel pour gérer l'action du bouton "Retour".
 * @param viewModel L'instance du `GameViewModel` partagé, fournie par Hilt et le NavGraph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel // Reçoit le ViewModel partagé via la navigation
) {
    // On observe l'état de l'UI du ViewModel de manière sécurisée par rapport au cycle de vie.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // On observe l'état pour afficher le dialogue de fin de partie
    if (uiState is GameUiState.Playing) {
        val playingState = uiState as GameUiState.Playing

        if (playingState.gameData.status == GameStatus.FINISHED) {
            // Affiche le dialogue par-dessus le reste
            EndGameDialog(
                players = playingState.gameData.players.sortedByDescending { it.score },
                onDismiss = onNavigateBack // Quand on ferme le dialogue, on exécute la navigation retour
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partie en cours") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        // --- AIGUILLAGE DE L'UI EN FONCTION DE L'ÉTAT ---
        // Le 'when' est le cœur de l'affichage réactif. Il garantit que l'UI
        // correspond toujours à l'état actuel du ViewModel.
        when (val state = uiState) {
            is GameUiState.Loading -> {
                // Si l'état est "Loading", on affiche une simple roue de chargement.
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
                GameLayout(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
                if (state.jokerSelectionState != null) {
                    JokerSelectionDialog(
                        onLetterSelected = { letter ->
                            viewModel.onJokerLetterSelected(letter)
                        },
                        onDismiss = {
//                        viewModel.onJokerSelectionDismissed()
                        }
                    )
                }
            }


            is GameUiState.Error -> {
                // Si une erreur se produit, on affiche un message clair et un bouton pour quitter.
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

/**
 * Nouveau Composable intermédiaire qui contient TOUT ce qui est lié au Drag & Drop.
 */
@Composable
private fun GameLayout(
    state: GameUiState.Playing,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // 5. C'EST ICI que l'on fournit le DragDropManager.
    //    Il n'a que deux enfants : GameContent et l'overlay de la tuile flottante.
    ProvideDragDropManager { dragDropManager ->
        // On utilise une Box pour superposer le jeu et l'overlay de la tuile.
        Box(modifier = Modifier.fillMaxSize()) {
            // Le contenu du jeu (plateau, chevalet, etc.)
            GameContent(
                gameData = state.gameData,
                localPlayerId = state.localPlayerId,
                selectedTileIndex = state.selectedTileIndex,
                // On passe le dragDropManager aux composants enfants qui en ont besoin.
                dragDropManager = dragDropManager,
                // On passe les fonctions du ViewModel comme des callbacks.
                // L'UI enfant appellera ces fonctions sans savoir ce qu'elles font.
                onTilePlacedFromRack = viewModel::onTilePlacedFromRack,
                onTileMovedOnBoard = viewModel::onTileMovedOnBoard,
                onTileReturnedToRack = viewModel::onTileReturnedToRack,
                onRackTilesReordered = viewModel::onRackTilesReordered,
                onPlayMove = viewModel::onPlayMove,
                onPass = viewModel::onPass,
                onUndoMove = viewModel::onUndoMove,
                onShuffleRack = viewModel::onShuffleRack,
                getValidDropBoardPositions = viewModel::getValidDropPositions,
                modifier = modifier
            )

            // --- AFFICHAGE DE L'OVERLAY DE DRAG ---
            // Cet élément est un enfant direct de ProvideDragDropManager, mais il est déclaré
            // APRÈS le Scaffold. Dans un Composable qui n'est pas un 'Row' ou 'Column',
            // cela signifie qu'il sera dessiné PAR-DESSUS le Scaffold.
            // C'est crucial pour que la tuile "flotte" au-dessus de toute l'UI.
            if (dragDropManager.state.isDragging && dragDropManager.state.draggedTile != null) {
                val dragState = dragDropManager.state
                val tileSize = 60.dp
                val tileSizePx = with(LocalDensity.current) { tileSize.toPx() }

                // On affiche la tuile en cours de déplacement avec un effet visuel
                // (agrandie, transparente, avec une ombre) pour la distinguer.
                TileView(
                    tile = dragState.draggedTile!!,
                    size = tileSize,
                    modifier = Modifier
                        // 1. On positionne la tuile avec offset
                        .offset {
                            IntOffset(
                                x = (dragState.currentCoordinates.x - tileSizePx / 2).toInt(),
                                y = (dragState.currentCoordinates.y - tileSizePx).toInt()
                            )
                        }
                        // 2. On applique les effets visuels
                        .graphicsLayer {
                            scaleX = 1.3f
                            scaleY = 1.3f
                            shadowElevation = 16f
                            alpha = 0.7f
                        }
                )
                // du Box}
            }
        }
    }
}