package club.djipi.lebarcs.ui.screens.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import club.djipi.lebarcs.ui.screens.game.GameUiState
import club.djipi.lebarcs.ui.screens.game.GameViewModel

/**
 * Le `Composable` de l'écran "Lobby" ou "Salle d'attente".
 *
 * Cet écran a un double rôle :
 * 1.  Pour l'utilisateur : Lui montrer qu'il a bien rejoint la partie et qu'une
 *     connexion est en cours d'établissement. Il peut voir qui est dans la partie.
 * 2.  Pour l'application : C'est l'étape technique où la connexion WebSocket
 *     avec le serveur de jeu est initiée via le `GameViewModel` partagé.
 *
 * Il est crucial de noter que cet écran utilise le MÊME `GameViewModel` que l'écran de jeu (`GameScreen`).
 * Ce ViewModel est partagé grâce au graphe de navigation imbriqué (`navigation(...)` dans `NavGraph.kt`),
 * ce qui garantit la persistance de la connexion et de l'état du jeu lors de la transition
 * vers l'écran suivant.
 *
 * @param gameId L'identifiant de la partie à rejoindre, reçu via la navigation.
 * @param viewModel L'instance du `GameViewModel` partagé, fournie par Hilt via le `NavGraph`.
 * @param onNavigateToGame Fonction de rappel pour naviguer vers l'écran de jeu principal (`GameScreen`).
 * @param onNavigateBack Fonction de rappel pour revenir à l'écran précédent.
 */
@Composable
fun LobbyScreen(
    gameId: String,
    viewModel: GameViewModel,
    onNavigateToGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // On observe l'état de l'UI (`GameUiState`) depuis le ViewModel partagé.
    // `collectAsStateWithLifecycle` est une version sécurisée de `collectAsState` qui
    // arrête la collecte de l'état lorsque l'UI n'est plus visible, économisant ainsi
    // les ressources.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Interface utilisateur simple pour la salle d'attente.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Salle d'attente", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Code de la partie : $gameId")

        Spacer(modifier = Modifier.height(32.dp))

        // On affiche un retour visuel à l'utilisateur en fonction de l'état
        // actuel de la connexion, observé depuis le `uiState`.
        when (val state = uiState) {
            // État initial ou pendant que la connexion s'établit.
            is GameUiState.Loading -> {
                CircularProgressIndicator()
                Text("Connexion en cours...")
            }
            // Si la connexion a échoué.
            is GameUiState.Error -> {
                Text(
                    "Erreur de connexion : ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            // Si la connexion est réussie et que le serveur a renvoyé le premier état du jeu.
            is GameUiState.Playing -> {
                Text("✅ Connecté !", color = Color(0xFF008000))
                Text("Joueurs dans la partie : ${state.gameData.players.size}")
                // TODO: Afficher la liste des noms des joueurs.
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToGame,
            // Le bouton pour "Commencer" n'est activé que lorsque la connexion est
            // un succès et que le ViewModel a reçu et traité l'état initial du jeu
            // (c'est-à-dire, quand l'état est `GameUiState.Playing`).
            enabled = uiState is GameUiState.Playing
        ) {
            Text("Commencer la partie")
        }
    }
}

