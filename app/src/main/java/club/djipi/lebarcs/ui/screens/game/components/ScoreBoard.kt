package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.Player
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

/**
 * Composant affichant les scores des joueurs de manière compacte.
 */
@Composable
fun ScoreBoard(
    players: List<Player>,
    currentPlayerId: String, // ID du joueur dont c'est le tour
    localPlayerId: String,   // ID du joueur qui utilise l'appareil
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // On espace les cartes de joueur uniformément
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // On trie les joueurs pour que "moi" soit toujours en premier
        val sortedPlayers = players.sortedByDescending { it.id == localPlayerId }

        sortedPlayers.forEach { player ->
            // Le 'weight' permet aux cartes de partager l'espace disponible.
            PlayerScoreCard(
                player = player,
                isCurrentTurn = player.id == currentPlayerId,
                isLocalPlayer = player.id == localPlayerId,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlayerScoreCard(
    player: Player,
    isCurrentTurn: Boolean,
    isLocalPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation subtile de la couleur de la bordure
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn) MaterialTheme.colorScheme.primary else Color.LightGray,
        animationSpec = tween(500)
    )

    Column(
        modifier = modifier
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isLocalPlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nom du joueur (tronqué si trop long)
        Text(
            text = if (isLocalPlayer) "Moi" else player.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Score
        Text(
            text = "${player.score}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Indicateur de tour : un petit point qui s'allume
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isCurrentTurn) borderColor else Color.Transparent)
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
            currentPlayerId = 3.toString(),
            localPlayerId = 1.toString(),
            modifier = Modifier.padding(16.dp)
        )
    }
}