package club.djipi.lebarcs.data.repository

import club.djipi.lebarcs.di.ApplicationScope
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.network.ClientToServerEvent
import club.djipi.lebarcs.shared.network.PlayMovePayload
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class CreateGameRequest(val creatorId: String)

@Serializable
private data class CreateGameResponse(val gameId: String)

class GameRepositoryImpl @Inject constructor(
    private val webSocketClient: WebSocketClient,
    // On injecte un scope de coroutine qui vivra aussi longtemps que l'application
    @ApplicationScope private val externalScope: CoroutineScope,
    private val httpClient: HttpClient
) : GameRepository {

    // On stocke le flux d'événements
    private val _events = MutableSharedFlow<ServerToClientEvent>()

    override fun connect(gameId: String, playerId: String) {
        // On lance l'écoute dans un scope externe pour qu'elle ne meure pas
        externalScope.launch {
            try {
                webSocketClient.connect(gameId, playerId).collect { event ->
                    _events.emit(event)
                }
            } catch (e: Exception) {
                // On peut émettre un événement d'erreur si on le souhaite
                e.printStackTrace()
            }
        }
    }

    override fun getEvents(): Flow<ServerToClientEvent> = _events.asSharedFlow()

    override suspend fun sendPlayMove(placedTiles: List<PlacedTile>) {
        val payload = PlayMovePayload( placedTiles )
        // 2. On crée l'événement en lui passant le payload
        val playMoveEvent = ClientToServerEvent.PlayMove(payload)
        // 3. (Optionnel) On peut mettre l'UI dans un état d'attente.
        // Par exemple, en désactivant les boutons en attendant la réponse du serveur.
        // Cela évite que l'utilisateur ne clique partout.
        webSocketClient.sendEvent(playMoveEvent)
    }

    override suspend fun createGame(creatorId: String): String {
        try {
            // On fait un appel POST à notre API serveur
            val response: CreateGameResponse = httpClient.post("http://djipi.club:8080/api/games") {
                contentType(ContentType.Application.Json)
                // On met l'ID du créateur dans le corps de la requête
                setBody(CreateGameRequest(creatorId = creatorId))
            }.body()

            println("GameRepository: Partie réelle créée avec l'ID ${response.gameId}")
            return response.gameId

        } catch (e: Exception) {
            println("GameRepository: Erreur lors de la création de la partie: ${e.message}")
            throw e // On propage l'erreur pour que le ViewModel la gère
        }
    }

    override fun close() {
        webSocketClient.close()
    }
}