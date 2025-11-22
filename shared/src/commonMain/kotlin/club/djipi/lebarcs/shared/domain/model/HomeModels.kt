package club.djipi.lebarcs.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerGameSummary(
    val gameId: String,
    val players: List<String>,
    val currentPlayerId: String?,
    val status: GameStatus,
    val turnNumber: Int
)