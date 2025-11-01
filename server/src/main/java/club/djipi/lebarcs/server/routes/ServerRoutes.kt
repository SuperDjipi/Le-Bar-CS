package club.djipi.lebarcs.server.routes

import club.djipi.lebarcs.server.service.GameManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.gameRoutes(gameManager: GameManager) {
    
    webSocket("/game/{gameId}") {
        val gameId = call.parameters["gameId"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Game ID required"))
            return@webSocket
        }
        
        val playerId = call.parameters["playerId"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player ID required"))
            return@webSocket
        }
        
        println("Player $playerId connected to game $gameId")
        
        // Enregistrer le joueur dans la partie
        gameManager.addPlayerToGame(gameId, playerId, this)
        
        try {
            // Envoyer l'état initial du jeu
            gameManager.sendGameState(gameId, playerId)
            
            // Écouter les messages du client
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    gameManager.handleMessage(gameId, playerId, text)
                }
            }
        } catch (e: Exception) {
            println("Error in WebSocket: ${e.localizedMessage}")
        } finally {
            println("Player $playerId disconnected from game $gameId")
            gameManager.removePlayerFromGame(gameId, playerId)
        }
    }
    
    // Route pour créer une nouvelle partie
    webSocket("/game/create") {
        val playerId = call.parameters["playerId"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player ID required"))
            return@webSocket
        }
        
        val playerName = call.parameters["playerName"] ?: "Player"
        
        val gameId = gameManager.createGame(playerId, playerName)
        send(Frame.Text("""{"type":"GAME_CREATED","gameId":"$gameId"}"""))
        
        close(CloseReason(CloseReason.Codes.NORMAL, "Game created"))
    }
}
