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

@Composable
fun DroppableBoardCell(
    cell: BoardCell,
    dragDropManager: DragDropManager? = null,
    modifier: Modifier = Modifier,
    size: Dp = 35.dp,
    content: @Composable BoxScope.() -> Unit
) {
    var cellPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val dragState = dragDropManager!!.state
    val localDensity = LocalDensity.current

    // On crée UNE SEULE variable qui définit si la cellule est une cible de drop valide.
    val isDropTarget by remember(dragState.isDragging, dragState.currentCoordinates, cell.tile) {
        derivedStateOf {
            // Conditions pour être une cible : le drag est actif ET la cellule est vide
            if (!dragState.isDragging || cell.tile != null) {
                false
            } else {
                // Et la souris est bien au-dessus de la cellule
                val cellSize = with(localDensity) { size.toPx() }
                val dragPos = dragState.currentCoordinates
                dragPos.x >= cellPosition.x && dragPos.x <= cellPosition.x + cellSize &&
                        dragPos.y >= cellPosition.y && dragPos.y <= cellPosition.y + cellSize
            }
        }
    }

    // Animation de la bordure, ne dépend plus que de `isDropTarget`
    val borderColor by animateColorAsState(
        targetValue = if (isDropTarget) Color(0xFF00FF00) else Color.Transparent,
        label = "borderColor"
    )

    // Animation de l'échelle, ne dépend plus que de `isDropTarget`
    val scale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.1f else 1f,
        label = "scale"
    )

    // Le LaunchedEffect devient beaucoup plus simple.
    LaunchedEffect(isDropTarget) {
        if (isDropTarget) {
            // Si on est une cible, on se déclare.
            dragDropManager.setTarget(DropTarget.Board(cell.position))
            Log.d(TAG, "Cible valide définie : ${cell.position}")
        } else {
            // Si je ne suis PLUS une cible, je vérifie si j'étais la cible précédente.
            // Si c'est le cas, et seulement dans ce cas, j'annule la cible.
            if (dragDropManager.state.target == DropTarget.Board(cell.position)) {
                dragDropManager.setTarget(null)
                Log.d(TAG, "Cible annulée car plus survolée : ${cell.position}")
            }
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
                width = if (isDropTarget) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(2.dp)
            )
    ) {
        // On affiche soit la tuile, soit la cellule de fond.
        // On utilise 'let' pour exécuter du code seulement si 'cell.tile' n'est pas null.
        // 'it' à l'intérieur du bloc 'let' est garanti d'être non-null (type 'Tile').
        cell.tile?.let { tile ->
            // S'il y a une tuile, on affiche TileView.
            // 'tile' ici est de type 'Tile', pas 'Tile?'. L'erreur est résolue.
            TileView(
                tile = tile,
                size = size,
                // On rend la tuile "draggable" pour permettre le déplacement depuis le plateau
                dragDropManager = dragDropManager,
                source = DragSource.Board(cell.position),
                enabled = !cell.isLocked
            )
        } ?: run {
            // Le '?: run' est l'équivalent du 'else'. Il s'exécute si 'cell.tile' EST null.
            // S'il n'y a pas de tuile, on affiche la cellule de fond.
            BoardCellView(
                cell = cell,
                size = size
            )
        }
    }
}
