package club.djipi.lebarcs.data.repository

import club.djipi.lebarcs.shared.domain.model.PlayerGameSummary
import club.djipi.lebarcs.shared.domain.repository.HomeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

/**
 * Implémentation du HomeRepository.
 * Parle aux API REST qui concernent l'écran d'accueil.
 */
class HomeRepositoryImpl @Inject constructor(
    // On injecte le client HTTP que notre NetworkModule sait déjà fournir.
    private val httpClient: HttpClient
) : HomeRepository {

    /**
     * Appelle l'API serveur pour obtenir la liste des parties actives d'un joueur.
     */
    override suspend fun getMyActiveGames(playerId: String): List<PlayerGameSummary> {
        try {
            // On fait un appel GET à notre nouvelle route d'API.
            // Ktor va automatiquement parser la réponse JSON dans une List<PlayerGameSummary>.
            val games = httpClient.get("http://djipi.club:8080/api/players/$playerId/games")
                .body<List<PlayerGameSummary>>()
            println("HomeRepository: ${games.size} partie(s) active(s) récupérée(s) pour le joueur $playerId")
            return games
        } catch (e: Exception) {
            // Gérer les erreurs réseau (serveur inaccessible, pas de connexion, etc.)
            println("HomeRepository: Erreur lors de la récupération des parties : ${e.message}")
            // On propage l'erreur pour que le ViewModel puisse l'afficher à l'utilisateur.
            throw e
        }
    }
}