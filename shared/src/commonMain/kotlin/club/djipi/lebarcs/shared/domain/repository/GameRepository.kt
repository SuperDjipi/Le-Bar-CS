package club.djipi.lebarcs.shared.domain.repository

import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import kotlinx.coroutines.flow.Flow

// Dans : shared/.../domain/repository/GameRepository.kt
interface GameRepository {
    fun connect(gameId: String)
    fun getEvents(): Flow<ServerToClientEvent> // Pour écouter les messages
    suspend fun sendPlayMove(placedTiles: List<PlacedTile>)
    suspend fun createGame(): String
    fun close()
}