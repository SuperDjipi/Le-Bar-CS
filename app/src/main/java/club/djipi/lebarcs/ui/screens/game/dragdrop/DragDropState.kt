package club.djipi.lebarcs.ui.screens.game.dragdrop

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile

/**
 * État du drag & drop
 */
data class DragDropState(
    val isDragging: Boolean = false,
    val draggedTile: Tile? = null,
    val draggedFromRackIndex: Int? = null,
    val dragOffset: Offset = Offset.Zero,
    val targetPosition: Position? = null
)

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

    fun startDrag(tile: Tile, fromRackIndex: Int, offset: Offset = Offset.Zero) {
        state = DragDropState(
            isDragging = true,
            draggedTile = tile,
            draggedFromRackIndex = fromRackIndex,
            dragOffset = offset
        )
    }

    fun updateDragPosition(offset: Offset) {
        if (state.isDragging) {
            state = state.copy(dragOffset = offset)
        }
    }

    fun setTargetPosition(position: Position?) {
        if (state.isDragging) {
            state = state.copy(targetPosition = position)
        }
    }

    fun endDrag(): DragDropResult? {
        val result = if (state.isDragging && state.targetPosition != null) {
            DragDropResult(
                tile = state.draggedTile!!,
                fromRackIndex = state.draggedFromRackIndex!!,
                toPosition = state.targetPosition!!
            )
        } else {
            null
        }

        state = DragDropState()
        return result
    }

    fun cancelDrag() {
        state = DragDropState()
    }
}

/**
 * Résultat d'un drag & drop réussi
 */
data class DragDropResult(
    val tile: Tile,
    val fromRackIndex: Int,
    val toPosition: Position
)

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