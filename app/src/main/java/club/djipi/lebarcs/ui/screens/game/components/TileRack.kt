package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi // Import crucial pour l'animation
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.BoardPosition
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.ui.screens.game.dragdrop.DropTarget
import club.djipi.lebarcs.ui.theme.LeBarCSTheme
import kotlin.math.min

/**
 * Chevalet du joueur contenant jusqu'à 7 tuiles
 */
@OptIn(ExperimentalFoundationApi::class) // <-- Annotation nécessaire pour animateItemPlacement
@Composable
fun TileRack(
    tiles: List<Tile>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    dragDropManager: DragDropManager? = null,
    onTileDragStart: (Int) -> Unit = {},
    onTileDragEnd: (Int) -> Unit = {},
    getValidBoardPositions: () -> Set<BoardPosition>
) {
    // Calculer la taille des tuiles en fonction de la largeur d'écran
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Largeur disponible = 90% de l'écran - padding - bordures
    val availableWidth = screenWidth * 0.9f - 32.dp // padding et bordures
    // On divise par le nombre de slots (7) et on soustrait l'espacement
    val horizontalPadding = 16.dp // Padding total du chevalet (8.dp de chaque côté)
    val spacing = 4.dp // Espacement entre les tuiles
    val calcCellSize = ((availableWidth - horizontalPadding - (spacing * 6)) / 7)

    val tileSize = min(calcCellSize.value, 60.dp.value).dp

    val dragState = dragDropManager?.state
    var rackBounds by remember { mutableStateOf<Rect?>(null) }
// On calcule si le curseur est au-dessus du chevalet pendant un drag
    val isHoveredOverRack = if (dragState != null && dragState.isDragging && rackBounds != null) {
        dragState.currentCoordinates in rackBounds!!
    } else {
        false
    }
    // NOUVEAU : Calculer l'index cible précis
    val targetRackIndex = if (dragState != null && dragState.isDragging && rackBounds != null) {
        val currentDragPosition = dragState.currentCoordinates
        val bounds = rackBounds!!

        if (currentDragPosition in bounds) {
            val relativeX = currentDragPosition.x - bounds.left
            (relativeX / (bounds.width / 7)).toInt().coerceIn(0, 6)
        } else {
            null
        }
    } else {
        null
    }

// On notifie le manager quand on survole le chevalet
    LaunchedEffect(targetRackIndex) {
        if (targetRackIndex != null && dragDropManager != null) {
            dragDropManager.setTarget(DropTarget.Rack(targetRackIndex))
        } else if (dragDropManager?.state?.target is DropTarget.Rack) {
            dragDropManager.setTarget(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.98f) // 95% de la largeur d'écran
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .background(Color(0xFF8B7355), RoundedCornerShape(8.dp))
            .border(3.dp, Color(0xFF654321), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(tileSize) // On donne une hauteur au LazyRow
                .onGloballyPositioned { layoutCoordinates ->
                    rackBounds = layoutCoordinates.boundsInWindow()
                }
        ) {
            // On utilise itemsIndexed pour avoir l'index
            itemsIndexed(
                items = tiles,
                // La clé est cruciale pour que Compose identifie chaque tuile
                // de manière unique et stable, même si son index change.
                key = { _, tile -> tile.id })
            { index, tile ->
                val isTargetSlot = targetRackIndex == index
                val slotScale by animateFloatAsState(
                    targetValue = if (isTargetSlot) 1.15f else 1f,
                    label = "slotScale"
                )
                Box(
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer {
                            scaleX = slotScale
                            scaleY = slotScale
                        }
                ) {
                    if (isTargetSlot && dragState?.source !is DragSource.Rack) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(tileSize)
                                .background(Color(0xFF00FF00))
                                .align(Alignment.CenterStart)
                        )
                    }
                    TileView(
                        tile = tile,
                        dragDropManager = dragDropManager,
                        source = DragSource.Rack(index),
                        size = tileSize,
                        isSelected = selectedIndex == index,
                        onDragStart = { onTileDragStart(index) },
                        onDragEnd = { onTileDragEnd(index) },
                        getValidPositions = getValidBoardPositions
                    )
                }
            }

            // On ajoute les emplacements vides à la fin
            val emptySlots = 7 - tiles.size
            if (emptySlots > 0) {
                items(emptySlots) { slotIndex ->
                        val actualIndex = tiles.size + slotIndex
                        val isTargetSlot = targetRackIndex == actualIndex

                        EmptyTileSlot(
                            size = tileSize,
                            isHighlighted = isTargetSlot,
                            modifier = Modifier.animateItem()
                        )
                    }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun TileRackPreview() {
    LeBarCSTheme {
        var selectedIndex by remember { mutableStateOf<Int?>(null) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chevalet plein
            TileRack(
                tiles = listOf(
                    Tile(letter = "S", points = 1),
                    Tile(letter = "C", points = 3),
                    Tile(letter = "R", points = 1),
                    Tile(letter = "A", points = 1),
                    Tile(letter = "B", points = 3),
                    Tile(letter = "L", points = 1),
                    Tile(letter = "E", points = 1)
                ),
                selectedIndex = selectedIndex,
                dragDropManager = null,
                onTileDragStart = { selectedIndex = it },
                onTileDragEnd = { selectedIndex = null },

                getValidBoardPositions = {emptySet()}
            )

            // Chevalet avec quelques tuiles
            TileRack(
                tiles = listOf(
                    Tile(letter = "H", points = 4),
                    Tile(letter = "E", points = 1),
                    Tile(letter = "L", points = 1),
                    Tile(letter = "_", points = 0, isJoker = true)
                ),
                getValidBoardPositions = {emptySet()},

            )
        }
    }
}