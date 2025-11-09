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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.Player
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

/**
 * Composant affichant les scores des joueurs
 */
@Composable
fun ScoreBoard(
    players: List<Player>,
    currentPlayerIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Scores",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        players.forEachIndexed { index, player ->
            PlayerScoreRow(
                player = player,
                isCurrentPlayer = index == currentPlayerIndex,
                rank = index + 1
            )
        }
    }
}

@Composable
private fun PlayerScoreRow(
    player: Player,
    isCurrentPlayer: Boolean,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCurrentPlayer) {
        Color(0xFFFFE4B5)
    } else {
        Color.White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(
                width = if (isCurrentPlayer) 2.dp else 1.dp,
                color = if (isCurrentPlayer) Color(0xFFFF8C00) else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rang
            Text(
                text = "#$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666)
            )

            // Nom du joueur
            Column {
                Text(
                    text = player.name,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )

                if (isCurrentPlayer) {
                    Text(
                        text = "À votre tour !",
                        fontSize = 12.sp,
                        color = Color(0xFFFF8C00),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Score
        Text(
            text = "${player.score} pts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScoreBoardPreview() {
    LeBarCSTheme {
        ScoreBoard(
            players = listOf(
                Player(id = "1", name = "Alice", score = 145, rack = emptyList()),
                Player(id = "2", name = "Bob", score = 132, rack = emptyList()),
                Player(id = "3", name = "Charlie", score = 98, rack = emptyList()),
                Player(id = "4", name = "Diana", score = 87, rack = emptyList())
            ),
            currentPlayerIndex = 0,
            modifier = Modifier.padding(16.dp)
        )
    }
}