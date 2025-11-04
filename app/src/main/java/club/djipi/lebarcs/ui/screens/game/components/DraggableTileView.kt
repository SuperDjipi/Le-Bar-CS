package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager

/**
 * Tuile draggable depuis le chevalet
 * Ne bouge plus elle-même, notifie juste le DragDropManager
 */
private const val TAG = "DraggableTileView"

@Composable
fun DraggableTileView(
    tile: Tile,
    rackIndex: Int,
    dragDropManager: DragDropManager,
    modifier: Modifier = Modifier,
    size: Dp = 55.dp,
    isSelected: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    var tilePosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                // Mémoriser la position de la tuile dans la fenêtre
                tilePosition = coordinates.positionInWindow()
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { touchOffset ->
                        // Position initiale = position de la tuile + où on a touché
                        val initialPosition = tilePosition + touchOffset
                        dragDropManager.startDrag(tile, rackIndex, initialPosition)
                        onDragStart()
                        Log.d(TAG,"DRAG START: Pos=${initialPosition}.")
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Position actuelle = position précédente + déplacement
                        val newPosition = dragDropManager.state.currentPosition + dragAmount
                        Log.d(TAG,"DRAG: Pos=${newPosition}.")
                        dragDropManager.updateDragPosition(newPosition)
                    },
                    onDragEnd = {
                        Log.d(TAG,"DRAG END: Action terminée.")
                        dragDropManager.endDrag()
                        onDragEnd()
                    },
                    onDragCancel = {
                        Log.d(TAG,"DRAG CANCEL: Action annulée.")
                        dragDropManager.cancelDrag()
                        onDragEnd()
                    }
                )
            }
    ) {
        // Afficher la tuile normalement (elle ne bouge plus)
        // C'est l'overlay dans GameContent qui affichera la copie draggée
        val isDragging = dragDropManager.state.isDragging &&
                dragDropManager.state.draggedFromRackIndex == rackIndex

        if (!isDragging) {
            // Afficher la tuile seulement si elle n'est pas en cours de drag
            TileView(
                tile = tile,
                size = size,
                isSelected = isSelected,
                enabled = true
            )
        }
    }
}