package club.djipi.lebarcs.ui.screens.game.dragdrop

import android.util.Log
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
    val initialPosition: Offset = Offset.Zero,  // Position de départ
    val currentPosition: Offset = Offset.Zero,  // Position actuelle
    val targetPosition: Position? = null
)

/**
 * CompositionLocal pour partager l'état du drag & drop
 */
val LocalDragDropState = compositionLocalOf {
    mutableStateOf(DragDropState())
}
private const val TAG = "DragDropManager"

/**
 * Gestionnaire du drag & drop
 */
class DragDropManager {
    var state by mutableStateOf(DragDropState())
        private set

    fun startDrag(tile: Tile, fromRackIndex: Int, initialPosition: Offset) {
        state = DragDropState(
            isDragging = true,
            draggedTile = tile,
            draggedFromRackIndex = fromRackIndex,
            initialPosition = initialPosition,
            currentPosition = initialPosition
        )
        Log.d(TAG, "startDrag: ${tile}, $fromRackIndex, $initialPosition")
    }

    fun updateDragPosition(currentPosition: Offset) {
        if (state.isDragging) {
            state = state.copy(currentPosition = currentPosition)
        }
    }

    fun setTargetPosition(position: Position?) {
        if (state.isDragging) {
            state = state.copy(targetPosition = position)
        }
    }

    fun endDrag(): DragDropResult? {
//        val result = if (state.isDragging && state.targetPosition != null) {
//            DragDropResult(
//                tile = state.draggedTile!!,
//                fromRackIndex = state.draggedFromRackIndex!!,
//                toPosition = state.targetPosition!!
//            )
//        } else {
//            null
//        }
        // C'est ici qu'on gère le drop !
        // 1. On crée une variable pour garder le résultat, initialisée à null.
        val result: DragDropResult? = if (state.isDragging && state.targetPosition != null) {
            // Si le drop est valide, on crée l'objet DragDropResult.
            Log.d(TAG, "Drop de ${state.draggedTile!!} sur la position ${state.targetPosition!!} !")
            DragDropResult(
                tile = state.draggedTile!!,
                fromRackIndex = state.draggedFromRackIndex!!,
                toPosition = state.targetPosition!!
            )
        } else {
            // Sinon, le résultat reste null.
            null
        }

        // 2. On réinitialise l'état DANS TOUS LES CAS.
        state = DragDropState()

        // 3. On retourne le résultat (soit le DragDropResult, soit null).
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