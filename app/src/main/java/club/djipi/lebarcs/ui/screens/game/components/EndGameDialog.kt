package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import club.djipi.lebarcs.shared.domain.model.Player

/**
 * Dialogue affiché à la fin de la partie.
 *
 * Présente le classement final des joueurs avec leurs scores.
 *
 * @param players La liste des joueurs, déjà triée par score décroissant.
 * @param onDismiss Callback appelé pour fermer le dialogue (bouton ou tap à l'extérieur).
 */
@Composable
fun EndGameDialog(
    players: List<Player>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titre
                Text(
                    text = "Partie terminée !",
                    style = MaterialTheme.typography.headlineMedium, // Un peu plus grand
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Liste des joueurs (classement)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(players) { index, player ->
                        PlayerScoreRow(
                            rank = index + 1,
                            player = player
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bouton pour retourner à l'accueil
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retour à l'accueil")
                }
            }
        }
    }
}

/**
 * Affiche une ligne du classement avec le rang, le nom et le score du joueur.
 */
@Composable
private fun PlayerScoreRow(
    rank: Int,
    player: Player
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$rank. ${player.name}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${player.score} points",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal,
            color = if (rank == 1) Color(0xFFD4A574) else MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EndGameDialogPreview() {
    val samplePlayers = listOf(
        Player(id = "1", name = "Djipi", score = 254),
        Player(id = "2", name = "Alpha", score = 210),
        Player(id = "3", name = "Claude", score = 180)
    )
    MaterialTheme {
        EndGameDialog(
            players = samplePlayers,
            onDismiss = {}
        )
    }
}