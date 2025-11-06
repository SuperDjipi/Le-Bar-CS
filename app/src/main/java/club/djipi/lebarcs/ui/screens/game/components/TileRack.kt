package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi // Import crucial pour l'animation
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onTileClick: (Int) -> Unit = {},
    onTileDragStart: (Int) -> Unit = {},
    onTileDragEnd: (Int) -> Unit = {}
) {
    // Calculer la taille des tuiles en fonction de la largeur d'écran
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Largeur disponible = 90% de l'écran - padding - bordures
    val availableWidth = screenWidth * 0.9f - 32.dp // padding et bordures
    // Taille d'une tuile = largeur disponible / 7 tuiles - espacements
    val calcCellSize = (availableWidth - (8.dp * 6)).value / 7  // 6 espaces de 8dp entre 7 tuiles

    val tileSize = min( calcCellSize, 60.dp.value).dp // Maximum 60dp par tuile
// --- DÉBUT DE L'AJOUT DE LA LOGIQUE DE DROP ---

    val dragState = dragDropManager?.state
    var rackBounds by remember { mutableStateOf<Rect?>(null) }

// On calcule si le curseur est au-dessus du chevalet pendant un drag
    val isHoveredOverRack = if (dragState != null && dragState.isDragging && rackBounds != null) {
        dragState.currentCoordinates in rackBounds!!
    } else {
        false
    }

// On notifie le manager quand on survole le chevalet
    LaunchedEffect(isHoveredOverRack) {
        if (isHoveredOverRack) {
            val currentDragPosition = dragState!!.currentCoordinates
            val bounds = rackBounds!!

            // Calcule l'index approximatif sous la souris
            val relativeX = currentDragPosition.x - bounds.left
            val targetIndex = (relativeX / (bounds.width / 7)).toInt().coerceIn(0, 6)

            // On se déclare comme cible de type Rack
            dragDropManager.setTarget(DropTarget.Rack(targetIndex))
        } else {
            // Si on n'est plus sur le chevalet et que la cible était le chevalet, on l'annule
            if (dragDropManager?.state?.target is DropTarget.Rack) {
                dragDropManager.setTarget(null)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.95f) // 95% de la largeur d'écran
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .background(Color(0xFF8B7355), RoundedCornerShape(8.dp))
            .border(3.dp, Color(0xFF654321), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Votre chevalet",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (dragDropManager != null) {
                    Text(
                        text = "Maintenez pour déplacer",
                        fontSize = 10.sp,
                        color = Color(0xFFE8DCC4),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tileSize) // On donne une hauteur au LazyRow
                    .onGloballyPositioned { layoutCoordinates ->
                        rackBounds = layoutCoordinates.boundsInWindow()
                    }
                    // On ajoute un retour visuel pour le survol
                    .border(
                        width = if (isHoveredOverRack) 3.dp else 0.dp,
                        color = if (isHoveredOverRack) Color.Green else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // On utilise itemsIndexed pour avoir l'index
                itemsIndexed(
                    items = tiles,
                    // La clé est cruciale pour que Compose identifie chaque tuile
                    // de manière unique et stable, même si son index change.
                    key = { _, tile -> tile.id })
                { index, tile ->
                    // Ce modifier est la clé de la fluidité. Il dit à Compose
                    // d'animer le changement de place de cette tuile.
                    Box(modifier = Modifier.animateItem()) {
                        TileView(
                            tile = tile,
                            dragDropManager = dragDropManager,
                            source = DragSource.Rack(index),
                            size = tileSize,
                            isSelected = selectedIndex == index,
                            onDragStart = { onTileDragStart(index) },
                            onDragEnd = { onTileDragEnd(index) }
                        )
                    }
                }

                // On ajoute les emplacements vides à la fin
                val emptySlots = 7 - tiles.size
                if (emptySlots > 0) {
                    items(emptySlots) {
                        EmptyTileSlot(size = tileSize)
                    }
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
                    Tile(letter='S', points=1),
                    Tile(letter='C', points=3),
                    Tile(letter='R', points=1),
                    Tile(letter='A', points=1),
                    Tile(letter='B', points=3),
                    Tile(letter='L', points=1),
                    Tile(letter='E', points=1)
                ),
                selectedIndex = selectedIndex,
                onTileClick = { selectedIndex = if (selectedIndex == it) null else it }
            )

            // Chevalet avec quelques tuiles
            TileRack(
                tiles = listOf(
                    Tile(letter='H', points=4),
                    Tile(letter='E', points=1),
                    Tile(letter='L', points=1),
                    Tile(letter='_', points=0, isJoker = true)
                )
            )
        }
    }
}