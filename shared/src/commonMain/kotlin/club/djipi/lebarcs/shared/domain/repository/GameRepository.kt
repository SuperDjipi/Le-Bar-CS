package club.djipi.lebarcs.shared.domain.repository

import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    // Connexion et écoute
    fun connect(gameId: String, playerId: String)
    fun getEvents(): Flow<ServerToClientEvent>

    // Actions de jeu
    suspend fun sendStartGame()
    suspend fun sendPlayMove(placedTiles: List<PlacedTile>)
    suspend fun sendPassTurn()

    // Actions hors-jeu (HTTP)
    suspend fun createGame(creatorId: String): String
    suspend fun joinGame(gameId: String, playerId: String)

    // Nettoyage
    fun close()
}
