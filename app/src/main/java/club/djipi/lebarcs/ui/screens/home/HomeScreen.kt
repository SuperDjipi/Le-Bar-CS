package club.djipi.lebarcs.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    onNavigateToLobby: (String) -> Unit,
    onNavigateToTestUI: () -> Unit = {},  // ← AJOUTÉ
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

        Spacer(modifier = Modifier.height(48.dp))

        // Créer une partie
        Button(
            onClick = { viewModel.createGame() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Créer une partie")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rejoindre une partie
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
            enabled = !uiState.isLoading && uiState.gameIdInput.isNotBlank()
        ) {
            Text("Rejoindre une partie")
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { onNavigateToTestUI() },  // ← Nouveau callback
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎨 Tester l'interface")
        }
        // Afficher les erreurs
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Naviguer vers le lobby quand un game ID est disponible
        LaunchedEffect(uiState.createdGameId) {
            uiState.createdGameId?.let { gameId ->
                onNavigateToLobby(gameId)
                viewModel.resetState()
            }
        }
    }
}