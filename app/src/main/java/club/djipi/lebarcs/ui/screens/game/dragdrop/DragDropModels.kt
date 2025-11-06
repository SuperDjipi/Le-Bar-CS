package club.djipi.lebarcs.ui.screens.game.dragdrop

import androidx.compose.ui.geometry.Offset
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile

/**
 * État du drag & drop
 */
data class DragDropState(
    val isDragging: Boolean = false,
    val isDropped: Boolean = false,
    val draggedTile: Tile? = null,
    val dragOffset: Offset = Offset.Zero,
    val currentCoordinates: Offset = Offset.Zero,
    val source: DragSource? = null,
    val target: DropTarget? = null
)

// NOUVEAU : Une classe scellée pour représenter toutes les sources possibles
sealed class DragSource {
    data class Rack(val index: Int) : DragSource()
    data class Board(val position: Position) : DragSource()
}

//* Représente la destination (cible) potentielle d'une tuile qui est "droppée".
sealed class DropTarget {
    /** La cible est une case du plateau. */
    data class Board(val position: Position) : DropTarget()
    /** La cible est le chevalet. On peut stocker l'index approximatif si nécessaire. */
    data class Rack(val index: Int) : DropTarget()
}