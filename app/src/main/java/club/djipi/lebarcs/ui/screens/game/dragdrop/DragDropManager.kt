package club.djipi.lebarcs.ui.screens.game.dragdrop

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import club.djipi.lebarcs.shared.domain.model.BoardPosition
import club.djipi.lebarcs.shared.domain.model.Tile

private const val TAG = "DragDropManager"

/**
 * CompositionLocal pour partager l'état du drag & drop
 */
val LocalDragDropState = compositionLocalOf {
    mutableStateOf(DragDropState())
}

/**
 * Gestionnaire du drag & drop
 */
class DragDropManager {
    var state by mutableStateOf(DragDropState())
        private set

    // Chevalet : limites et enregistrement
    private var rackBounds: Rect? = null

    fun registerRackArea(bounds: Rect) {
        this.rackBounds = bounds
    }

    fun startDrag(
        tile: Tile,
        source: DragSource,
        startCoordinates: Offset,
        validPositions: Set<BoardPosition> = emptySet()
    ) {
        state = DragDropState(
            isDragging = true,
            draggedTile = tile,
            source = source,
            currentCoordinates = startCoordinates,
            validDropBoardPositions = validPositions
        )
        Log.d(TAG, "startDrag: ${tile}, $source, $startCoordinates")
    }

    fun updateDragPosition(dragCoordinates: Offset) {
        if (state.isDragging) {
            state = state.copy(currentCoordinates = dragCoordinates)
        }
    }

    fun setTarget(newDropTarget: DropTarget?, isValid: Boolean = true) {
        if (state.isDragging) {
            // Extraire la position pour la preview
            val previewPos = when (newDropTarget) {
                is DropTarget.Board -> newDropTarget.boardPosition
                else -> null
            }

            state = state.copy(
                target = newDropTarget,
                ghostPreviewBoardPosition = previewPos,
                isValidDrop = isValid
            )
        }
    }

    fun endDrag() {
        val isDropped = state.isDragging && state.target != null

        if (isDropped) {
            // Log de débogage plus précis
            Log.d(TAG, "Drop réussi de la tuile ${state.draggedTile?.letter} sur la cible ${state.target}")
        } else {
            Log.d(TAG, "Drop annulé (${if (!state.isValidDrop) "invalide" else "pas de cible"})")
        }

        state = state.copy(
            isDragging = false,
            isDropped = isDropped
        )
    }

    fun cancelDrag() {
        state = DragDropState()
    }

    fun consumeDropEvent() {
        state = DragDropState()
    }
}

/**
 * Composable pour fournir le gestionnaire de drag & drop
 */
@Composable
fun ProvideDragDropManager(
    content: @Composable (DragDropManager) -> Unit
) {
    val manager = remember { DragDropManager() }
    content(manager)
}