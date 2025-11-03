package club.djipi.lebarcs.data.remote

import androidx.navigation.safe.args.generator.ErrorMessage
import club.djipi.lebarcs.shared.dto.GameStateDto
import club.djipi.lebarcs.shared.dto.GameStateUpdate
import club.djipi.lebarcs.shared.dto.PlayerJoined
import club.djipi.lebarcs.shared.dto.ServerEvent
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    private var session: DefaultClientWebSocketSession? = null
    private val _gameState = MutableStateFlow<GameStateDto?>(null)
    val gameState: StateFlow<GameStateDto?> = _gameState.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    suspend fun connect(serverUrl: String, gameId: String) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            
            session = client.webSocketSession("$serverUrl/game/$gameId")
            
            _connectionState.value = ConnectionState.CONNECTED
            
            // Écouter les messages entrants
            session?.let { session ->
                while (session.isActive) {
                    when (val frame = session.incoming.receive()) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            handleMessage(text)
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            e.printStackTrace()
        } finally {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    private fun handleMessage(text: String) {
        try {
//            val message = json.decodeFromString<MessageDto>(text)
//            when (message.type) {
//                "GAME_STATE" -> {
//                    message.gameState?.let { _gameState.value = it }
//                }
//                "PLAYER_JOINED" -> {
//                    // Gérer l'arrivée d'un joueur
//                }
//                "MOVE_PLAYED" -> {
//                    // Gérer un coup joué
//                }
//                "GAME_ENDED" -> {
//                    // Gérer la fin de partie
//                }
//            }
            // La magie opère ici : on décode directement dans la classe scellée
            when (val event = json.decodeFromString<ServerEvent>(text)) {

                is GameStateUpdate -> {
                    // Le compilateur sait que 'event' est de type GameStateUpdate
                    // 'event.gameState' est garanti non-null ici !
                    _gameState.value = event.gameState
                }

                is PlayerJoined -> {
                    // Le compilateur sait que 'event.player' existe et n'est pas null
                    println("Un joueur a rejoint : ${event.player.name}")
                    // Mettez à jour votre UI ou votre état local ici
                }

                is ErrorMessage -> {
                    // Le compilateur sait que 'event.message' existe
                    println("Erreur du serveur : ${event.message}")
                }

                // Ajoutez un 'is' pour chaque type d'événement
                is club.djipi.lebarcs.shared.dto.ErrorMessage -> TODO()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun sendMove(moveData: String) {
        session?.send(Frame.Text(moveData))
    }
    
    suspend fun disconnect() {
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
