package club.djipi.lebarcs.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.local.UserPreferencesRepository
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * La `data class` qui représente l'intégralité de l'état de l'écran d'accueil (`HomeScreen`).
 *
 * C'est un modèle immuable (toutes les propriétés sont des `val`).
 * Pour modifier l'état, on doit créer une nouvelle instance avec la méthode `.copy()`.
 * Cette approche garantit un flux de données unidirectionnel et prévisible.
 *
 * @property isLoading `true` si une opération asynchrone (comme la création de partie) est en cours.
 * @property localPlayerId L'identifiant unique et persistant du joueur sur cet appareil.
 * @property playerName Le nom du joueur, tel qu'enregistré. `null` si le joueur n'est pas encore inscrit.
 * @property requiresOnboarding `true` si le `playerName` est `null`, indiquant à l'UI d'afficher l'écran d'inscription.
 * @property gameIdInput Le texte actuellement entré par l'utilisateur dans le champ "Code de la partie".
 * @property createdGameId Si une partie vient d'être créée, contient son ID. Déclenche la navigation automatique.
 * @property error Un message d'erreur à afficher à l'utilisateur, le cas échéant.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val localPlayerId: String? = null,
    val playerName: String? = null,
    val requiresOnboarding: Boolean = false,
    val gameIdInput: String = "",
    val createdGameId: String? = null,
    val error: String? = null
)

/**
 * Le `ViewModel` pour l'écran d'accueil (`HomeScreen`).
 *
 * Ce ViewModel agit comme un pont entre l'UI (qui est "bête") et la logique métier
 * et les sources de données de l'application.
 *
 * Il est annoté avec `@HiltViewModel` pour que Hilt puisse le créer et lui injecter
 * automatiquement ses dépendances (`UserPreferencesRepository`, `GameRepository`).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    /**
     * Le `StateFlow` privé et mutable qui contient l'état actuel de l'UI.
     * Seul le ViewModel peut le modifier (`private`).
     */
    private val _uiState = MutableStateFlow(HomeUiState())

    /**     * Le `StateFlow` public et immuable, exposé à l'UI.
     * L'UI peut s'abonner à ce `Flow` pour observer les changements d'état
     * et se recomposer automatiquement.
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Le bloc d'initialisation, exécuté une seule fois lors de la création du ViewModel.
     * Son rôle est de lancer les tâches de fond initiales, comme la vérification
     * de l'identité du joueur.
     */
    init {
        // On lance une coroutine dans le scope du ViewModel, qui sera automatiquement
        // annulée lorsque le ViewModel sera détruit.
        viewModelScope.launch {
            // Tâche 1 : Observer le nom du joueur en continu.
            // `collect` est une fonction suspendue qui écoute les émissions du Flow.
            userPreferencesRepository.playerName.collect { name ->
                if (name == null) {
                    // Si le nom n'est pas trouvé dans le DataStore, on met l'état
                    // pour demander l'inscription à l'utilisateur.
                    _uiState.update { it.copy(requiresOnboarding = true, playerName = null) }
                } else {
                    // Sinon, on met à jour l'état avec le nom du joueur.
                    _uiState.update { it.copy(requiresOnboarding = false, playerName = name) }
                }
            }
        }
        viewModelScope.launch {
            // Tâche 2 : Récupérer l'ID persistant du joueur.
            // C'est essentiel pour que la navigation puisse fonctionner.
            val playerId = userPreferencesRepository.getLocalPlayerId()
            _uiState.update { it.copy(localPlayerId = playerId) }
        }
    }

    /**
     * Lance la création d'une nouvelle partie en appelant le `GameRepository`.
     */
    fun createGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Délègue la logique de création de partie au Repository.
                val gameId = gameRepository.createGame()
                // Si l'appel réussit, on met à jour l'état avec le nouvel ID de partie,
                // ce qui déclenchera la navigation via le `LaunchedEffect` dans l'UI.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        createdGameId = gameId
                    )
                }
            } catch (e: Exception) {
                // En cas d'erreur, on met à jour l'état pour afficher un message.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur lors de la création de la partie: ${e.message}"
                    )
                }
            }
        }
    }
    /**
     * Gère la modification du texte dans le champ de saisie du code de la partie.
     */
    fun onGameIdChanged(newValue: String) {
        // `.update` est un moyen sûr de modifier le StateFlow de manière atomique.
        _uiState.update { it.copy(gameIdInput = newValue.uppercase()) }
    }

    /**
     * Enregistre le nom de l'utilisateur lors de la première inscription.
     */
    fun submitOnboarding(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                // On sauvegarde le nom dans le DataStore via le repository.
                userPreferencesRepository.savePlayerName(name)
                // 2. On récupère l'ID du joueur local
                val playerId = userPreferencesRepository.getLocalPlayerId()
                // L'observation continue (le `collect` dans `init`) se chargera
                // automatiquement de mettre à jour l'UI et de faire disparaître l'écran d'inscription.

                // --- Logique d'envoi au serveur ---
                // TODO: Créer un service/repository pour gérer cet appel.
                // Exemple conceptuel :
                // val payload = RegisterProfilePayload(name = name)
                // val event = ClientToServerEvent.RegisterProfile(payload)
                // userRepository.registerProfile(playerId, event)
                Log.d("HomeViewModel", "TODO: Envoyer le profil (nom: $name, id: $playerId) au serveur.")
                // ------------------------------------
            }
        }    }

    /**
     * Réinitialise l'ID de la partie créée pour éviter les re-navigations automatiques.
     * Cette fonction est appelée par l'UI juste après que la navigation a été initiée.
     */
    fun resetNavigation() {
        _uiState.update { it.copy(createdGameId = null) }
    }
}

