package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.ui.theme.LeBarCSTheme
import kotlin.math.min

/**
 * Chevalet du joueur contenant jusqu'à 7 tuiles
 */
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Afficher les 7 emplacements
                for (i in 0 until 7) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i < tiles.size) {
                                TileView(
                                    tile = tiles[i],
                                    dragDropManager = dragDropManager,
                                    source = DragSource.Rack(i),
                                    size = tileSize,
                                    isSelected = selectedIndex == i,
                                    onDragStart = { onTileDragStart(i) },
                                    onDragEnd = { onTileDragEnd(i) }
                                )
                        } else {
                            // Emplacement vide
                            EmptyTileSlot(size = tileSize)
                        }
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
                    Tile('S', 1),
                    Tile('C', 3),
                    Tile('R', 1),
                    Tile('A', 1),
                    Tile('B', 3),
                    Tile('L', 1),
                    Tile('E', 1)
                ),
                selectedIndex = selectedIndex,
                onTileClick = { selectedIndex = if (selectedIndex == it) null else it }
            )

            // Chevalet avec quelques tuiles
            TileRack(
                tiles = listOf(
                    Tile('H', 4),
                    Tile('E', 1),
                    Tile('L', 1),
                    Tile('_', 0, isJoker = true)
                )
            )
        }
    }
}