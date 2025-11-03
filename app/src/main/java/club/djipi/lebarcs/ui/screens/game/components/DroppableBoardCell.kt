package club.djipi.lebarcs.ui.screens.game.components

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
    val isHovered by remember {
        derivedStateOf {
            if (!dragState.isDragging) return@derivedStateOf false

            val dragX = cellPosition.x + dragState.dragOffset.x
            val dragY = cellPosition.y + dragState.dragOffset.y

            // Vérifier si la position du drag est dans les limites de la case
            val cellSize = with(localDensity) { size.toPx() }
            dragX >= cellPosition.x && dragX <= cellPosition.x + cellSize &&
                    dragY >= cellPosition.y && dragY <= cellPosition.y + cellSize
        }
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
            dragDropManager.setTargetPosition(cell.position)
        } else if (!isHovered && dragState.targetPosition == cell.position) {
            dragDropManager.setTargetPosition(null)
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