package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.ui.screens.game.GameData
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.ui.screens.game.dragdrop.DropTarget

private const val TAG = "GameContent"

@Composable
fun GameContent(
    gameData: GameData,
    selectedTileIndex: Int?,
    dragDropManager: DragDropManager,
    onTileSelected: (Int) -> Unit,
    onCellClick: (Position) -> Unit,
    onTilePlacedFromRack: (rackIndex: Int, position: Position) -> Unit,
    onTileMovedOnBoard: (from: Position, to: Position) -> Unit,
    onTileReturnedToRack: (from: Position) -> Unit,
    onRackTilesReordered: (from: Int, to: Int) -> Unit,
    onShuffleRack: () -> Unit,
    onUndoMove: () -> Unit,
    onPlayMove: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observer les résultats du drag & drop
    LaunchedEffect(dragDropManager.state) {
        val state = dragDropManager.state
        if (state.isDropped) {
            // On a besoin de connaître la source ET la cible
            val source = state.source
            val target = state.target
            if (source is DragSource.Rack && target is DropTarget.Board) {
                // On a placé une tuile sur le plateau
                Log.d(TAG, "D&d terminé (Rack->Board) : source=$source, target=$target")
                onTilePlacedFromRack(source.index, target.position)
            }
            if (source is DragSource.Board && target is DropTarget.Board) {
                // On a déplacé une tuile sur le plateau
                onTileMovedOnBoard(source.position, target.position)
            }
            if (source is DragSource.Board && target is DropTarget.Rack) {
                // Logique pour remettre sur le chevalet...
                Log.d(TAG, "Drop détecté : Plateau -> Chevalet")
                onTileReturnedToRack(source.position)
            }
            if (source is DragSource.Rack && target is DropTarget.Rack) {
                // Logique pour réorganiser le chevalet...
                Log.d(TAG, "Drop détecté : Chevalet -> Chevalet")
                if (source.index != target.index) { // On ne fait rien si on drop sur soi-même
                    onRackTilesReordered(source.index, target.index)
                }
            }

            dragDropManager.consumeDropEvent()
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bouton Mélanger
            OutlinedButton(
                onClick = onShuffleRack,
                modifier = Modifier.weight(1f)
            ) {
                //Text("Mélanger")
                Icon(Icons.Default.Refresh, "Mélanger")
            }

            // Bouton Annuler
            OutlinedButton(
                onClick = onUndoMove,
                modifier = Modifier.weight(1f),
                // Le bouton n'est actif que si des tuiles ont été posées
                enabled = gameData.placedTiles.isNotEmpty()
            ) {
                //Text("Annuler")
                Icon(Icons.Default.ArrowDropDown, "Annuler")
            }

            // Bouton Passer
            OutlinedButton(
                onClick = onPass,
                modifier = Modifier.weight(1f)
            ) {
                //Text("Passer")
                Icon(Icons.Default.Close, "Passer")
            }

            // Bouton Jouer (le plus important)
            Button(
                onClick = onPlayMove,
                modifier = Modifier.weight(1f),
                enabled = gameData.isCurrentMoveValid
            ) {
                Icon(Icons.Default.Done, "Jouer")
                Text("${gameData.currentMoveScore}")
            }

        }
    }
}
