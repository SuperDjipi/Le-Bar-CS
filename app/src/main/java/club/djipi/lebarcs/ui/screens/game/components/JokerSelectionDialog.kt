package club.djipi.lebarcs.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Dialog pour la sélection d'une lettre pour un joker.
 *
 * Affiche une grille élégante de toutes les lettres de l'alphabet
 * dans un style qui rappelle les tuiles de Scrabble.
 *
 * @param onLetterSelected Callback appelé quand l'utilisateur sélectionne une lettre
 * @param onDismiss Callback appelé pour fermer le dialog (bouton Annuler ou tap à l'extérieur)
 */
@Composable
fun JokerSelectionDialog(
    onLetterSelected: (Char) -> Unit,
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
                    text = "Choisissez une lettre",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "pour votre joker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Grille de lettres
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(('A'..'Z').toList()) { letter ->
                        JokerLetterTile(
                            letter = letter,
                            onClick = { onLetterSelected(letter) }
                        )
                    }
                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Bouton Annuler
//                TextButton(
//                    onClick = onDismiss,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text("Annuler")
//                }
            }
        }
    }
}

/**
 * Une tuile individuelle représentant une lettre dans le style Scrabble.
 *
 * Design inspiré des tuiles de Scrabble :
 * - Fond beige/crème
 * - Bordure légèrement arrondie
 * - Ombre pour donner un effet 3D
 * - Lettre en gras et centrée
 * - Effet de hover/press pour le feedback
 */
@Composable
private fun JokerLetterTile(
    letter: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Couleurs style Scrabble
    val tileColor = Color(0xFFF5E6D3) // Beige/crème
    val borderColor = Color(0xFFD4A574) // Bordure dorée
    val letterColor = Color(0xFF2C1810) // Marron foncé pour la lettre

    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = tileColor,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = letterColor
        )
    }
}

/**
 * Preview pour tester le dialog dans Android Studio
 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun JokerSelectionDialogPreview() {
    MaterialTheme {
        JokerSelectionDialog(
            onLetterSelected = {},
            onDismiss = {}
        )
    }
}