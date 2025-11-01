package club.djipi.lebarcs.data.remote

import club.djipi.lebarcs.data.remote.dto.GameStateDto
import club.djipi.lebarcs.data.remote.dto.MessageDto
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
            val message = json.decodeFromString<MessageDto>(text)
            when (message.type) {
                "GAME_STATE" -> {
                    message.gameState?.let { _gameState.value = it }
                }
                "PLAYER_JOINED" -> {
                    // Gérer l'arrivée d'un joueur
                }
                "MOVE_PLAYED" -> {
                    // Gérer un coup joué
                }
                "GAME_ENDED" -> {
                    // Gérer la fin de partie
                }
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
