package cclub.djipi.lebarcs.server.service

import club.djipi.lebarcs.server.model.ServerGame
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GameManager {
    private val games = ConcurrentHashMap<String, ServerGame>()
    private val playerSessions = ConcurrentHashMap<String, MutableMap<String, WebSocketSession>>()
    private val mutex = Mutex()
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }
    
    /**
     * Créer une nouvelle partie
     */
    suspend fun createGame(hostPlayerId: String, hostPlayerName: String): String {
        val gameId = UUID.randomUUID().toString()
        
        mutex.withLock {
            val game = ServerGame(
                id = gameId,
                hostPlayerId = hostPlayerId
            )
            games[gameId] = game
            playerSessions[gameId] = mutableMapOf()
        }
        
        return gameId
    }
    
    /**
     * Ajouter un joueur à une partie
     */
    suspend fun addPlayerToGame(gameId: String, playerId: String, session: WebSocketSession) {
        mutex.withLock {
            val game = games[gameId] ?: run {
                session.send(Frame.Text("""{"type":"ERROR","error":"Game not found"}"""))
                return
            }
            
            playerSessions[gameId]?.put(playerId, session)
            
            // Notifier tous les joueurs
            broadcastToGame(gameId, """{"type":"PLAYER_JOINED","playerId":"$playerId"}""")
        }
    }
    
    /**
     * Retirer un joueur d'une partie
     */
    suspend fun removePlayerFromGame(gameId: String, playerId: String) {
        mutex.withLock {
            playerSessions[gameId]?.remove(playerId)
            
            // Notifier les autres joueurs
            broadcastToGame(gameId, """{"type":"PLAYER_LEFT","playerId":"$playerId"}""")
            
            // Supprimer la partie si elle est vide
            if (playerSessions[gameId]?.isEmpty() == true) {
                games.remove(gameId)
                playerSessions.remove(gameId)
            }
        }
    }
    
    /**
     * Envoyer l'état du jeu à un joueur
     */
    suspend fun sendGameState(gameId: String, playerId: String) {
        val game = games[gameId] ?: return
        val session = playerSessions[gameId]?.get(playerId) ?: return
        
        val gameState = game.toGameStateDto()
        val message = """{"type":"GAME_STATE","gameState":${json.encodeToString(gameState)}}"""
        
        session.send(Frame.Text(message))
    }
    
    /**
     * Gérer un message d'un joueur
     */
    suspend fun handleMessage(gameId: String, playerId: String, message: String) {
        try {
            // Parser le message
            // Traiter selon le type (PLAY_MOVE, EXCHANGE_TILES, PASS, etc.)
            
            // Pour l'instant, juste broadcaster
            broadcastToGame(gameId, message, excludePlayerId = playerId)
            
        } catch (e: Exception) {
            val session = playerSessions[gameId]?.get(playerId)
            session?.send(Frame.Text("""{"type":"ERROR","error":"${e.message}"}"""))
        }
    }
    
    /**
     * Diffuser un message à tous les joueurs d'une partie
     */
    private suspend fun broadcastToGame(
        gameId: String, 
        message: String, 
        excludePlayerId: String? = null
    ) {
        playerSessions[gameId]?.forEach { (playerId, session) ->
            if (playerId != excludePlayerId) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    println("Failed to send to player $playerId: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Obtenir une partie
     */
    fun getGame(gameId: String): ServerGame? = games[gameId]
}
