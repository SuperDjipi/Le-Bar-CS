package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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

/**
 * Case du plateau qui peut recevoir des tuiles par drag & drop
 */
private const val TAG = "DroppableBoardCell"

@Composable
fun DroppableBoardCell(
    cell: BoardCell,
    dragDropManager: DragDropManager,
    modifier: Modifier = Modifier,
    size: Dp = 35.dp,
    onClick: () -> Unit = {}
) {
    var cellPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val dragState = dragDropManager.state
    val localDensity = LocalDensity.current
    // Déterminer si la tuile draggée est au-dessus de cette case
//    val isHovered by remember {
//        derivedStateOf {
//            if (!dragState.isDragging) return@derivedStateOf false
//
//            val dragX = cellPosition.x + dragState.currentPosition.x
//            val dragY = cellPosition.y + dragState.currentPosition.y
//            Log.d(TAG, "dragX: $dragX, dragY: $dragY")
//
//
//            // Vérifier si la position du drag est dans les limites de la case
//            val cellSize = with(localDensity) { size.toPx() }
//            dragX >= cellPosition.x && dragX <= cellPosition.x + cellSize &&
//                    dragY >= cellPosition.y && dragY <= cellPosition.y + cellSize
//        }
//    }
    val isHovered = if (dragState.isDragging) {
        val currentDragPosition = dragState.currentPosition

        // Le calcul est beaucoup plus simple :
        // La position de la souris est-elle dans le rectangle de la cellule ?
        val cellSize = with(localDensity) { size.toPx() }

        // Log pour vérifier que les valeurs sont correctes
        Log.d(TAG, "Vérification survol: DragPos=${currentDragPosition}, CellPos=${cellPosition}, CellSize=${cellSize}")

        currentDragPosition.x >= cellPosition.x && currentDragPosition.x <= cellPosition.x + cellSize &&
                currentDragPosition.y >= cellPosition.y && currentDragPosition.y <= cellPosition.y + cellSize
    } else {
        false
    }

    // Animation de la bordure quand on survole
    val borderColor by animateColorAsState(
        targetValue = if (isHovered && cell.tile == null) Color(0xFF00FF00) else Color.Transparent,
        label = "borderColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        label = "scale"
    )

    // Notifier le manager quand on survole
    LaunchedEffect(isHovered) {
        if (isHovered && cell.tile == null) {
            // provoque une race condition
//            dragDropManager.setTargetPosition(cell.position)
//        } else if (!isHovered && dragState.targetPosition == cell.position) {
//            dragDropManager.setTargetPosition(null)
            // Si on survole cette cellule, on la déclare comme cible. Pas de else
            dragDropManager.setTargetPosition(cell.position)
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
            .then(
                if (isHovered && cell.tile == null) {
                    Modifier.border(3.dp, borderColor, RoundedCornerShape(2.dp))
                } else {
                    Modifier
                }
            )
    ) {
        BoardCellView(
            cell = cell,
            size = size,
            onClick = onClick
        )
    }
}