package club.djipi.lebarcs.ui.screens.game.dragdrop

import androidx.compose.ui.geometry.Offset
import club.djipi.lebarcs.shared.domain.model.BoardPosition
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
    val target: DropTarget? = null,
    // NOUVEAUX CHAMPS pour l'UX
    val validDropBoardPositions: Set<BoardPosition> = emptySet(), // Cases où on peut dropper
    val ghostPreviewBoardPosition: BoardPosition? = null, // Position de prévisualisation
    val isValidDrop: Boolean = true // Le drop actuel est-il valide ?
)

// NOUVEAU : Une classe scellée pour représenter toutes les sources possibles
sealed class DragSource {
    data class Rack(val index: Int) : DragSource()
    data class Board(val boardPosition: BoardPosition) : DragSource()
}

//* Représente la destination (cible) potentielle d'une tuile qui est "droppée".
sealed class DropTarget {
    /** La cible est une case du plateau. */
    data class Board(val boardPosition: BoardPosition) : DropTarget()
    /** La cible est le chevalet. On peut stocker l'index approximatif si nécessaire. */
    data class Rack(val index: Int) : DropTarget()
}