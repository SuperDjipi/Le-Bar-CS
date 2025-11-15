package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.BoardCell
import club.djipi.lebarcs.shared.domain.model.BonusType
import club.djipi.lebarcs.ui.theme.*

/**
 * Composant représentant une case du plateau de Scrabble
 */
@Composable
fun BoardCellView(
    cell: BoardCell,
    modifier: Modifier = Modifier,
    size: Dp = 35.dp
) {
    val (backgroundColor, text, textColor) = when (cell.bonus) {
        BonusType.NONE -> Triple(Color(0xFFE8DCC4), "", Color.Black)
        BonusType.DOUBLE_LETTER -> Triple(DoubleLetterColor, "LD", Color.White)
        BonusType.TRIPLE_LETTER -> Triple(TripleLetterColor, "LT", Color.White)
        BonusType.DOUBLE_WORD -> Triple(DoubleWordColor, "MD", Color.White)
        BonusType.TRIPLE_WORD -> Triple(TripleWordColor, "MT", Color.White)
        BonusType.CENTER -> Triple(CenterStarColor, "★", Color.Black)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(2.dp))
            .border(
                width = 0.5.dp,
                color = Color(0xFFAAAAAA),
                shape = RoundedCornerShape(2.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Si une tuile est posée sur la case
        if (cell.tile != null) {
            TileView(
                tile = cell.tile!!,
                size = (size * 0.85f),
                enabled = !cell.isLocked
            )
        } else if (text.isNotEmpty()) {
            // Afficher le bonus
            Text(
                text = text,
                fontSize = (size.value * 0.3).sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}