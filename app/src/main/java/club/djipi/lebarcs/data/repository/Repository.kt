package club.djipi.lebarcs.data.repository

import club.djipi.lebarcs.data.local.dao.GameDao
import club.djipi.lebarcs.data.local.dao.PlayerDao
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.shared.domain.model.GameState
import club.djipi.lebarcs.shared.domain.model.Move
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface GameRepository {
    suspend fun createGame(): String
    suspend fun joinGame(gameId: String, playerId: String)
    suspend fun checkGameExists(gameId: String): Boolean
    fun observeGame(gameId: String): Flow<GameState?>
    suspend fun playMove(gameId: String, move: Move)
    suspend fun disconnect()
}

class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val webSocketClient: WebSocketClient
) : GameRepository {
    
    override suspend fun createGame(): String {
        // TODO: Implémenter la création via WebSocket
        // Pour l'instant, retourner un ID fictif
        return "GAME-${System.currentTimeMillis()}"
    }
    
    override suspend fun joinGame(gameId: String, playerId: String) {
        // TODO: Se connecter au serveur WebSocket
        // webSocketClient.connect("ws://server:8080", gameId)
    }
    
    override suspend fun checkGameExists(gameId: String): Boolean {
        // TODO: Vérifier auprès du serveur
        return true
    }
    
    override fun observeGame(gameId: String): Flow<GameState?> {
        // Combiner les données locales et les mises à jour WebSocket
        return gameDao.getGame(gameId).map { entity ->
            // TODO: Convertir GameEntity en Game
            null
        }
    }
    
    override suspend fun playMove(gameId: String, move: Move) {
        // TODO: Envoyer le coup via WebSocket
        // val moveDto = move.toDto()
        // webSocketClient.sendMove(json.encodeToString(moveDto))
    }
    
    override suspend fun disconnect() {
        webSocketClient.close()
    }
}
