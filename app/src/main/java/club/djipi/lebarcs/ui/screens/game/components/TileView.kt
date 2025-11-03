package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

/**
 * Composant représentant une tuile de Scrabble
 */
@Composable
fun TileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val backgroundColor = when {
        !enabled -> Color(0xFFCCCCCC)
        isSelected -> Color(0xFFFFE4B5)
        else -> Color(0xFFF5DEB3) // Couleur beige du Scrabble
    }

    val borderColor = if (isSelected) Color(0xFFFF8C00) else Color(0xFF8B7355)

    Box(
        modifier = modifier
            .size(size)
            .shadow(if (enabled) 4.dp else 1.dp, RoundedCornerShape(4.dp))
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (tile.isJoker && tile.letter == '_') {
            // Joker vide
            Text(
                text = "?",
                fontSize = (size.value * 0.5).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B4513)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lettre
                Text(
                    text = tile.letter.toString(),
                    fontSize = (size.value * 0.5).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2F4F4F)
                )

                // Points (en petit en bas à droite)
                if (!tile.isJoker) {
                    Text(
                        text = tile.points.toString(),
                        fontSize = (size.value * 0.2).sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF696969),
                        modifier = Modifier.offset(x = (size.value * 0.25).dp, y = -(size.value * 0.1).dp)
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
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFFE8E8E8), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
    )
}

@Preview(showBackground = true)
@Composable
fun TileViewPreview() {
    LeBarCSTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TileView(tile = Tile('A', 1))
            TileView(tile = Tile('K', 10))
            TileView(tile = Tile('_', 0, isJoker = true))
            TileView(tile = Tile('E', 1), isSelected = true)
            TileView(tile = Tile('Z', 10), enabled = false)
        }
    }
}