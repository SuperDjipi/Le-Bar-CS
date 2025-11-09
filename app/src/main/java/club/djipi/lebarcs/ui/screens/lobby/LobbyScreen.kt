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

@Composable
fun LobbyScreen(
    gameId: String,
    viewModel: GameViewModel,
    onNavigateToGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // On observe l'état de l'UI depuis le ViewModel partagé
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- C'EST ICI QUE LA MAGIE OPÈRE ---
    // Ce bloc de code s'exécute une seule fois lorsque LobbyScreen s'affiche
    // (ou si gameId change, ce qui est parfait).
    LaunchedEffect(key1 = gameId) {
        // On dit au ViewModel de commencer à se connecter à la partie.
        println("LobbyScreen: Lancement de la connexion au serveur pour la partie $gameId...")
        viewModel.connectToGame(gameId)
    }
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

        // On affiche un retour visuel basé sur l'état de la connexion
        when (val state = uiState) {
            is GameUiState.Loading -> {
                CircularProgressIndicator()
                Text("Connexion en cours...")
            }
            is GameUiState.Error -> {
                Text("Erreur de connexion : ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            is GameUiState.Playing -> {
                Text("✅ Connecté !", color = Color(0xFF008000))
                Text("Joueurs dans la partie : ${state.gameData.players.size}")
                // On peut lister les joueurs ici, etc.
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToGame,
            // Le bouton "Commencer" n'est actif que si la connexion est réussie
            // et que l'état du jeu a été reçu du serveur.
            enabled = uiState is GameUiState.Playing
        ) {
            Text("Commencer la partie")
        }
    }
}

