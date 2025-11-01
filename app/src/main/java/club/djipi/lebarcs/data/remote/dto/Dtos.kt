package club.djipi.lebarcs.data.remote.dto

import club.djipi.lebarcs.domain.model.*
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val type: String,
    val gameState: GameStateDto? = null,
    val move: MoveDto? = null,
    val playerId: String? = null,
    val error: String? = null
)

@Serializable
data class GameStateDto(
    val gameId: String,
    val players: List<PlayerDto>,
    val currentPlayerIndex: Int,
    val tilesRemaining: Int,
    val status: String,
    val board: List<List<CellDto>>
)

@Serializable
data class PlayerDto(
    val id: String,
    val name: String,
    val score: Int,
    val rackSize: Int,  // On ne transmet pas les tuiles des autres joueurs
    val isActive: Boolean
)

@Serializable
data class CellDto(
    val row: Int,
    val col: Int,
    val tile: TileDto? = null,
    val bonus: String,
    val isLocked: Boolean
)

@Serializable
data class TileDto(
    val letter: Char,
    val points: Int,
    val isJoker: Boolean = false
)

@Serializable
data class MoveDto(
    val playerId: String,
    val tiles: List<PlacedTileDto>,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PlacedTileDto(
    val tile: TileDto,
    val row: Int,
    val col: Int
)

// Extensions pour convertir entre DTO et modèles du domaine
fun GameStateDto.toDomain(): Game {
    return Game(
        id = gameId,
        players = players.map { it.toDomain() },
        board = Board(), // À reconstruire depuis board
        currentPlayerIndex = currentPlayerIndex,
        tilesRemaining = tilesRemaining,
        status = GameStatus.valueOf(status)
    )
}

fun PlayerDto.toDomain(): Player {
    return Player(
        id = id,
        name = name,
        score = score,
        rack = emptyList(), // Les tuiles du chevalet ne sont pas transmises
        isActive = isActive
    )
}

fun TileDto.toDomain(): Tile {
    return Tile(
        letter = letter,
        points = points,
        isJoker = isJoker
    )
}

fun Tile.toDto(): TileDto {
    return TileDto(
        letter = letter,
        points = points,
        isJoker = isJoker
    )
}

fun Move.toDto(): MoveDto {
    return MoveDto(
        playerId = playerId,
        tiles = tiles.map { 
            PlacedTileDto(
                tile = it.tile.toDto(),
                row = it.position.row,
                col = it.position.col
            )
        },
        timestamp = timestamp
    )
}
