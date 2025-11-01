package club.djipi.lebarcs.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Composable
fun GameScreen(
    gameId: String,
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Partie en cours",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Game ID: $gameId")
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // TODO: Afficher le plateau de jeu ici
        
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quitter la partie")
        }
    }
}

@HiltViewModel
class GameViewModel @Inject constructor(
    // TODO: Injecter le repository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState
}

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(val gameData: String) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
