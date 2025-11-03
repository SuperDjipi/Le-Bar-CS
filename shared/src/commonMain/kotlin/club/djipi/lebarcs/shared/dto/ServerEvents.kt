package club.djipi.lebarcs.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerEvent

@Serializable
@SerialName("GAME_STATE")
data class GameStateUpdate(val gameState: GameStateDto) : ServerEvent()

@Serializable
@SerialName("ERROR")
data class ErrorMessage(val message: String) : ServerEvent()

@Serializable
@SerialName("PLAYER_JOINED")
data class PlayerJoined(val player: PlayerDto) : ServerEvent()