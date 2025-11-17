package club.djipi.lebarcs.data.repository

import androidx.activity.result.launch
import club.djipi.lebarcs.di.ApplicationScope
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.network.ClientToServerEvent
import club.djipi.lebarcs.shared.network.PlayMovePayload
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val webSocketClient: WebSocketClient,
    // On injecte un scope de coroutine qui vivra aussi longtemps que l'application
    @ApplicationScope private val externalScope: CoroutineScope
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

    override suspend fun createGame(): String {
        // TODO: Remplacer ceci par un véritable appel réseau au serveur Node.js
        // pour créer une nouvelle partie.

        // Pour l'instant, on simule la création d'une partie et on retourne un ID.
        // Cela permettra au HomeViewModel de fonctionner.
        println("GameRepository: Simulation de la création d'une partie...")
        val newGameId = (100..999).random().toString() // Génère un ID aléatoire comme "451"
        println("GameRepository: Partie simulée avec l'ID $newGameId")
        return newGameId
    }
    override fun close() {
        webSocketClient.close()
    }
}