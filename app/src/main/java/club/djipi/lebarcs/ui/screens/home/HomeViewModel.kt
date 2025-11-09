package club.djipi.lebarcs.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val gameIdInput: String = "",
    val createdGameId: String? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun createGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val gameId = gameRepository.createGame()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        createdGameId = gameId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors de la création de la partie: ${e.message}"
                    )
                }
            }
        }
    }

    fun joinGame() {
        val gameId = _uiState.value.gameIdInput
        if (gameId.isBlank()) return

        // On met simplement à jour l'état pour que le LaunchedEffect déclenche la navigation.
        _uiState.update {
            it.copy(
                // On ne gère plus isLoading ici, c'est le LobbyScreen qui s'en chargera.
                isLoading = false,
                createdGameId = gameId
            )
        }
    }

    fun onGameIdChanged(newValue: String) {
        _uiState.update { it.copy(gameIdInput = newValue.uppercase()) }
    }
    
    fun resetState() {
        _uiState.update { 
            HomeUiState()
        }
    }
}
