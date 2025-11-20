package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.BoardCell
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.ui.screens.game.dragdrop.DropTarget

private const val TAG = "DroppableBoardCell"

// Enum pour les états,visuels de case
private enum class CellStatus {
    Normal,           // Pas de drag en cours
    ValidAvailable,   // Case disponible (pré-indication)
    ValidTarget,      // Hover sur case valide
    InvalidTarget     // Hover sur case invalide
}

@Composable
fun DroppableBoardCell(
    cell: BoardCell,
    dragDropManager: DragDropManager? = null,
    modifier: Modifier = Modifier,
    size: Dp = 35.dp
) {
    var cellPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val dragState = dragDropManager!!.state
    val localDensity = LocalDensity.current

    // Vérifier si cette case est dans les positions valides
    val isValidPosition = dragState?.validDropBoardPositions?.contains(cell.boardPosition) == true

    // Vérifier si la souris survole cette case
    val isHovered by remember(dragState?.isDragging, dragState?.currentCoordinates, cell.tile) {
        derivedStateOf {
            if (dragState?.isDragging != true || cell.tile != null) {
                false
            } else {
                val cellSize = with(localDensity) { size.toPx() }
                val dragPos = dragState.currentCoordinates
                dragPos.x >= cellPosition.x && dragPos.x <= cellPosition.x + cellSize &&
                        dragPos.y >= cellPosition.y && dragPos.y <= cellPosition.y + cellSize
            }
        }
    }

    // Déterminer le statut visuel de la case
    val cellStatus = when {
        dragState?.isDragging != true -> CellStatus.Normal
        isHovered && isValidPosition -> CellStatus.ValidTarget
        isHovered && !isValidPosition -> CellStatus.InvalidTarget
        isValidPosition -> CellStatus.ValidAvailable
        else -> CellStatus.Normal
    }

    // Animations basées sur le statut
    val borderColor by animateColorAsState(
        targetValue = when (cellStatus) {
            CellStatus.ValidTarget -> Color(0xFF00FF00)      // Vert : hover valide
            CellStatus.InvalidTarget -> Color(0xFFFF0000)    // Rouge : hover invalide
            CellStatus.ValidAvailable -> Color(0x8000FF00)   // Vert pâle : disponible
            CellStatus.Normal -> Color.Transparent
        },
        label = "borderColor"
    )

    val scale by animateFloatAsState(
        targetValue = when (cellStatus) {
            CellStatus.ValidTarget -> 1.15f
            CellStatus.InvalidTarget -> 1.05f
            CellStatus.ValidAvailable -> 1.03f
            CellStatus.Normal -> 1f
        },
        label = "scale"
    )

    // Notifier le manager si on survole
    LaunchedEffect(isHovered, isValidPosition) {
        if (isHovered) {
            dragDropManager?.setTarget(
                DropTarget.Board(cell.boardPosition),
                isValid = isValidPosition
            )
        } else if (dragDropManager?.state?.target == DropTarget.Board(cell.boardPosition)) {
            dragDropManager.setTarget(null)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cellPosition = coordinates.positionInRoot()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Le modifier de la bordure est aussi simplifié
            .border(
                width = when (cellStatus) {
                    CellStatus.ValidTarget, CellStatus.InvalidTarget -> 3.dp
                    CellStatus.ValidAvailable -> 2.dp
                    CellStatus.Normal -> 0.dp
                },
                color = borderColor,
                shape = RoundedCornerShape(2.dp)
            )
    ) {
        // Ghost preview de la tuile
        if (dragState?.ghostPreviewBoardPosition == cell.boardPosition &&
            dragState.draggedTile != null &&
            cell.tile == null
        ) {
            TileView(
                tile = dragState.draggedTile!!,
                size = size,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.5f
                    scaleX = 0.85f
                    scaleY = 0.85f
                }
            )
        }

        // Contenu de la case
        cell.tile?.let { tile ->
            // S'il y a une tuile, on affiche TileView.
            TileView(
                tile = tile,
                size = size,
                dragDropManager = dragDropManager,
                source = DragSource.Board(cell.boardPosition),
                enabled = !cell.isLocked
            )
        } ?: run {
            BoardCellView(
                cell = cell,
                size = size
            )
        }
    }
}

