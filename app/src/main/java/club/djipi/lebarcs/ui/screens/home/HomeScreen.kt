package club.djipi.lebarcs.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Le `Composable` de l'écran d'accueil, premier point d'interaction pour l'utilisateur.
 *
 * Cet écran a deux responsabilités principales :
 * 1.  Gérer l'inscription (onboarding) du joueur s'il se connecte pour la première fois
 *     (c'est-à-dire, si son nom n'est pas encore enregistré).
 * 2.  Offrir les options pour créer ou rejoindre une partie existante.
 *
 * Il est entièrement piloté par l'état (`HomeUiState`) fourni par le `HomeViewModel`.
 *
 * @param onNavigateToLobby Une fonction de rappel (callback) qui sera invoquée pour
 *                          déclencher la navigation vers le flux de jeu. Elle transmet
 *                          l'ID de la partie et l'ID du joueur local.
 * @param viewModel L'instance du `HomeViewModel`, fournie automatiquement par Hilt.
 */
@Composable
fun HomeScreen(
    onNavigateToLobby: (gameId: String, playerId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // on s'abonne à l'état (UiState) du ViewModel. `collectAsState` garantit que
    // le Composable se recomposera à chaque nouvelle émission de l'état.
    val uiState by viewModel.uiState.collectAsState()

    // Aiguillage principal de l'UI : On affiche soit l'écran d'inscription, soit le menu.
    when {
        // Si le ViewModel nous dit que l'inscription est requise...
        uiState.requiresOnboarding -> {
            // ...on affiche l'écran dédié à l'inscription.
            OnboardingScreen(
                onNameSubmit = { name -> viewModel.submitOnboarding(name) }
            )
        }

        // Sinon (le joueur est déjà connu)...
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Le Bar CS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
//                Text(
//                    text = uiState.localPlayerId!!,
//                    style = MaterialTheme.typography.headlineLarge,
//                    fontWeight = FontWeight.Bold
//                )

                Spacer(modifier = Modifier.height(48.dp))

                // --- Section "Créer une partie" ---
                Button(
                    onClick = { viewModel.createGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    // Le bouton est désactivé pendant le chargement pour éviter les clics multiples.
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        // Affiche une roue de chargement à l'intérieur du bouton.
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Créer une partie")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Section "Rejoindre une partie" ---
                OutlinedTextField(
                    value = uiState.gameIdInput,
                    onValueChange = viewModel::onGameIdChanged,
                    label = { Text("Code de la partie") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.joinGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    // Le bouton est actif uniquement si un code a été entré ET si l'ID du joueur est chargé.
                    enabled = uiState.gameIdInput.isNotBlank() && uiState.localPlayerId != null
                ) {
                    Text("Rejoindre une partie")
                }

                // --- Affichage des erreurs ---
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // --- Logique de navigation automatique ---
                // `LaunchedEffect` est utilisé pour exécuter une action de type "effet de bord" (comme la navigation)
                // en réponse à un changement d'état, sans bloquer l'UI.
                // Il s'exécutera à chaque fois que `createdGameId` ou `localPlayerId` changera.
                LaunchedEffect(key1 = uiState.createdGameId, key2 = uiState.localPlayerId) {
                    val gameId = uiState.createdGameId
                    val playerId = uiState.localPlayerId

                    // On ne navigue que si les deux informations sont disponibles.
                    if (gameId != null && playerId != null) {
                        onNavigateToLobby(gameId, playerId)
                        // On notifie le ViewModel de réinitialiser l'état de navigation
                        // pour éviter une re-navigation si l'utilisateur revient sur cet écran.
                        viewModel.resetNavigation()
                    }
                }
            }
        }
    }
}

/**
 * Un `Composable` simple pour l'écran d'inscription (onboarding).
 *
 * @param onNameSubmit Une fonction de rappel invoquée lorsque l'utilisateur soumet son nom.
 */
@Composable
fun OnboardingScreen(onNameSubmit: (String) -> Unit) {
    // `remember` est utilisé pour stocker l'état local du champ de texte.
    var name by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenue ! Comment vous appelez-vous ?")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Votre nom") })
        Button(onClick = { onNameSubmit(name) }) {
            Text("Enregistrer")
        }
    }
}