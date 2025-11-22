package club.djipi.lebarcs.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import club.djipi.lebarcs.shared.domain.model.GameStatus
import club.djipi.lebarcs.shared.domain.model.PlayerGameSummary

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Effet pour gérer la navigation automatique lorsqu'une partie est créée ou rejointe.
    LaunchedEffect(uiState.createdGameId) {
        val gameId = uiState.createdGameId
        val playerId = uiState.localPlayerId

        if (gameId != null && playerId!= null) {
            onNavigateToLobby(gameId, playerId)
            // On notifie le ViewModel de réinitialiser l'état de navigation
            // pour ne pas re-naviguer si l'écran se recompose.
            viewModel.resetNavigation()
        }
    }

    // Affiche l'écran d'inscription si nécessaire.
    if (uiState.requiresOnboarding) {
        OnboardingScreen( { name -> viewModel.submitOnboarding(name) }
        )
    } else {
        // Affiche le tableau de bord principal si le joueur est reconnu.
        HomeScreenContent(
            uiState = uiState,
            viewModel = viewModel
        )
    }}

/**
 * Le contenu du tableau de bord principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Le Bar CS - Accueil") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenue, ${uiState.playerName ?: "Joueur"} !",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECTION POUR CRÉER/REJOINDRE UNE PARTIE ---
            GameJoinCreateSection(uiState = uiState, viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // --- DÉBUT DE L'AFFICHAGE DES PARTIES ACTIVES ---
            Text(
                text = "Mes parties en cours",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && uiState.activeGames.isEmpty()) {
                CircularProgressIndicator()
            } else if (uiState.activeGames.isEmpty()) {
                Text("Vous n'avez aucune partie en cours.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.activeGames, key = { it.gameId }) { game ->
                        GameSummaryCard(
                            game = game,
                            localPlayerId = uiState.localPlayerId ?: "",
                            localPlayerName = uiState.playerName ?: "",
                            onClick = {
                                viewModel.onGameSelected(game.gameId)
                            }
                        )                    }
                }
            }
            // --- FIN DE L'AFFICHAGE DES PARTIES ACTIVES ---
        }
    }
}

/**
 * Affiche une carte résumant une partie en cours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSummaryCard(
    game: PlayerGameSummary,
    localPlayerId: String,
    localPlayerName: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Partie #${game.gameId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Avec : ${game.players.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            val turnText = if (game.status == GameStatus.WAITING_FOR_PLAYERS) {
                "En attente de joueurs..."
            } else if (game.currentPlayerId == localPlayerId) {
                "C'est à votre tour !"
            } else {
                "Au tour de ${game.players.firstOrNull { it != localPlayerName } ?: "l'adversaire"}" // Logique à affiner
            }

            Text(
                text = turnText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (game.currentPlayerId == localPlayerId)
                    FontWeight.Bold else FontWeight.Normal,
                color = if (game.currentPlayerId == localPlayerId) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameJoinCreateSection(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
    }
    }
