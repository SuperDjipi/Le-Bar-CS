package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.ui.screens.game.GameData
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager


@Composable
fun GameContent(
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

    // Contenu principal
    Column(
        modifier = modifier
            .fillMaxSize(),
        //.padding(8.dp),
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
}
