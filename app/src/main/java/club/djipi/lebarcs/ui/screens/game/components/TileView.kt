package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import club.djipi.lebarcs.shared.domain.model.BoardPosition
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

private const val TAG = "TileView"
/**
 * Composant représentant une tuile de Scrabble
 */
@Composable
fun TileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    dragDropManager: DragDropManager? = null,
    source: DragSource? = null,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    getValidPositions: () -> Set<BoardPosition> = { emptySet() }
) {
    val backgroundColor = when {
        !enabled -> Color(0xFFCCCCCC)
        isSelected -> Color(0xFFFFE4B5)
        else -> Color(0xFFF5DEB3) // Couleur beige du Scrabble
    }
    val borderColor = if (isSelected) Color(0xFFFF8C00) else Color(0xFF8B7355)
    var tileCoordinates by remember { mutableStateOf(Offset.Zero) }
    // On crée un Modifier qui sera la base
    var finalModifier = modifier

    // On ajoute la logique de drag SEULEMENT si les paramètres sont fournis
    if (dragDropManager != null && source != null && enabled) {
        finalModifier = finalModifier
            .onGloballyPositioned { coordinates ->
                tileCoordinates = coordinates.positionInWindow()
            }
            .pointerInput(source) { // La clé est `source` pour que le geste soit réactif au changement de tuile/position
                detectDragGestures(
                    onDragStart = { touchOffset ->
                        val initialCoordinates = tileCoordinates + touchOffset
                        dragDropManager.startDrag(tile, source, initialCoordinates,validPositions = getValidPositions())
                        onDragStart() // callback dans TileRack
                        Log.d(TAG,"DRAG START: Source=${source}, Pos=${initialCoordinates}.")
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newCoordinates = dragDropManager.state.currentCoordinates + dragAmount
                        dragDropManager.updateDragPosition(newCoordinates)
                    },
                    onDragEnd = {
                        Log.d(TAG,"DRAG END: Action terminée.")
                        dragDropManager.endDrag()
                        onDragEnd() // callback dans TileRack
                    },
                    onDragCancel = {
                        Log.d(TAG,"DRAG CANCEL: Action annulée.")
                        dragDropManager.cancelDrag()
                        onDragEnd() // callback dans TileRack
                    }
                )
            }
    }
    Box(
        modifier = finalModifier
            .size(size)
            .shadow(if (enabled) 4.dp else 1.dp, RoundedCornerShape(4.dp))
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (tile.isJoker && tile.assignedLetter == null) {
            // Joker vide
            Text(
                text = "?",
                fontSize = (size.value * 0.5).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B4513)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center)  {
                // Lettre
                Text(
                    text = tile.displayLetter,
                    fontSize = (size.value * 0.8).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2F4F4F),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp)
                )

                // Points (en petit en bas à droite)
                if (!tile.isJoker) {
                    Text(
                        text = tile.points.toString(),
                        fontSize = (size.value * 0.25).sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F3F3F),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 1.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tuile vide (emplacement sur le plateau)
 */
@Composable
fun EmptyTileSlot(
    size: Dp,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFF00FF00) else Color(0x40FFFFFF),
        label = "emptySlotBorder"
    )

    Box(
        modifier = modifier
            .size(size)
            .border(
                width = if (isHighlighted) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
    )
}

//@Preview(showBackground = true)
//@Composable
//fun TileViewPreview() {
//    LeBarCSTheme {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            TileView(tile = Tile(letter = "A", points = 1))
//            TileView(tile = Tile(letter = "K", points = 10))
//            TileView(tile = Tile(letter = "_", points = 0, isJoker = true))
//            TileView(tile = Tile(letter = "E", points = 1), isSelected = true)
//            TileView(tile = Tile(letter = "Z", points = 10), enabled = false)
//        }
//    }
//}

@Preview(showBackground = true)
@Composable
fun TileViewSizeTestPreview() {
    LeBarCSTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("50dp (Default)")
                TileView(tile = Tile(letter = "Z", points = 10), size = 50.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("42dp (Small Phone)")
                TileView(tile = Tile(letter = "Z", points = 10), size = 42.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("60dp (Large Phone)")
                TileView(tile = Tile(letter = "Z", points = 10), size = 60.dp)
            }
        }
    }
}