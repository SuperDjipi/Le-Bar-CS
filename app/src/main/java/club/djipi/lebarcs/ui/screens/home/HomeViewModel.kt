package club.djipi.lebarcs.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.local.UserPreferencesRepository
import club.djipi.lebarcs.shared.domain.model.PlayerGameSummary
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.*
import kotlinx.serialization.Serializable

// --- DÉFINIR LES MODÈLES POUR L'APPEL API ---
@Serializable
private data class RegisterRequest(val name: String, val password: String)

@Serializable
private data class RegisterResponse(val message: String, val playerId: String)


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
    val error: String? = null,
    val activeGames: List<PlayerGameSummary> = emptyList()
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
    private val gameRepository: GameRepository,
    private val homeRepository: HomeRepository,
    private val httpClient: HttpClient
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
     * Charge l'identité complète du joueur (ID et Nom) en une seule opération.
     */
    init {
        viewModelScope.launch {
            // On lance une seule coroutine pour gérer tout le chargement initial.

            // 1. On récupère les deux informations de manière séquentielle.
            //    Comme ce sont des appels `suspend`, la coroutine attendra la fin de chaque
            //    appel avant de passer au suivant.
            val localPlayerId = userPreferencesRepository.getLocalPlayerId()
            val playerName = userPreferencesRepository.getPlayerName()

            // 2. On met à jour l'état de l'UI UNE SEULE FOIS avec toutes les informations.
            _uiState.update { currentState ->
                if (localPlayerId != null && playerName != null) {
                    // Si le joueur est déjà connu, on met à jour son profil complet.
                    currentState.copy(
                        localPlayerId = localPlayerId,
                        playerName = playerName,
                        requiresOnboarding = false // Le joueur n'a pas besoin de s'inscrire.
                    )
                } else {
                    // Si une des informations est manquante, c'est un nouvel utilisateur.
                    currentState.copy(
                        requiresOnboarding = true // On affiche l'écran d'inscription.
                    )
                }
            }
            // 3. Si le joueur est reconnu, on charge immédiatement ses parties actives.
            if (localPlayerId != null) {
                loadActiveGames(localPlayerId)
            }
            Log.d("HomeViewModel", "Chargement initial terminé. PlayerID: $localPlayerId, PlayerName: $playerName")
        }
    }

    /**
     * Charge la liste des parties actives pour le joueur local en appelant le HomeRepository.
     */
    private fun loadActiveGames(playerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // On peut montrer un loader pour cette action aussi
            try {
                // On délègue l'appel réseau au repository spécialisé.
                val games = homeRepository.getMyActiveGames(playerId)

                // On met à jour l'état de l'UI avec la liste des parties reçues.
                _uiState.update { it.copy(isLoading = false, activeGames = games) }
                Log.d("HomeViewModel", "${games.size} partie(s) active(s) chargée(s).")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erreur lors du chargement des parties actives", e)
                _uiState.update { it.copy(isLoading = false, error = "Impossible de charger vos parties.") }
            }
        }
    }
    /**
     * Lance la création d'une nouvelle partie en appelant le `GameRepository`.
     */
    fun createGame() {
        viewModelScope.launch {
            val playerId = uiState.value.localPlayerId
            if (playerId == null) {
                _uiState.update { it.copy(error = "L'ID du joueur local n'est pas encore chargé.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Délègue la logique de création de partie au Repository.
                val gameId = gameRepository.createGame(creatorId = playerId)
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

    fun joinGame() {
        viewModelScope.launch {
            val gameId = uiState.value.gameIdInput
            val playerId = uiState.value.localPlayerId

            if (gameId.isBlank() || playerId == null) {
                _uiState.update { it.copy(error = "Le code de la partie et l'ID joueur sont requis.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                gameRepository.joinGame(gameId, playerId)
                // Si l'appel réussit, on déclenche la navigation
                _uiState.update { it.copy(isLoading = false, createdGameId = gameId) } // On réutilise 'createdGameId' pour la navigation
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Impossible de rejoindre la partie : ${e.message}") }
            }
        }
    }

    /**
     * Gère la soumission du formulaire d'inscription (onboarding).
     * 1. Sauvegarde le nouveau profil (ID et Nom) localement.
     * 2. Enregistre ce nouveau profil sur le serveur via l'API REST.
     * 3. Met à jour l'état de l'UI pour passer à l'écran principal.
     */
    fun submitOnboarding(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                _uiState.update { it.copy(isLoading = true, error = null) }
                try {
                    // Pour l'instant, on utilise un mot de passe factice.
                    // Plus tard, vous pourrez ajouter un champ pour cela dans l'UI.
                    val dummyPassword = "password123"

                    // 1. On appelle l'API du serveur pour enregistrer le profil.
                    Log.d("HomeViewModel", "Tentative d'enregistrement du profil sur le serveur pour : $name")
                    val response: RegisterResponse = httpClient.post("http://djipi.club:8080/api/register") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterRequest(name = name, password = dummyPassword))
                    }.body()

                    // L'API a répondu avec succès. L'ID du joueur est celui généré par le serveur.
                    val newPlayerId = response.playerId
                    Log.d("HomeViewModel", "Profil enregistré sur le serveur. PlayerID reçu : $newPlayerId")

                    // 2. On sauvegarde ce profil (ID et Nom) localement sur l'appareil.
                    userPreferencesRepository.createAndSaveNewProfile(name, newPlayerId)

                    // 3. On met à jour l'état de l'UI pour refléter le succès.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            localPlayerId = newPlayerId,
                            playerName = name,
                            requiresOnboarding = false // L'inscription est terminée !
                        )
                    }

                } catch (e: Exception) {
                    // Gérer les erreurs (pseudo déjà pris, serveur inaccessible, etc.)
                    Log.e("HomeViewModel", "Erreur lors de l'inscription :", e)
                    _uiState.update { it.copy(isLoading = false, error = "Erreur lors de l'inscription : ${e.message}") }
                }
            }
        }
    }

    /**
     * Réinitialise l'ID de la partie créée pour éviter les re-navigations automatiques.
     * Cette fonction est appelée par l'UI juste après que la navigation a été initiée.
     */
    fun resetNavigation() {
        _uiState.update { it.copy(createdGameId = null) }
    }

    /**     * Appelé lorsque l'utilisateur clique sur une partie existante dans la liste.
     */
    fun onGameSelected(gameId: String) {
        _uiState.update { it.copy(createdGameId = gameId) }
    }
}