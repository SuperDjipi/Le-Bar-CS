package club.djipi.lebarcs.data.remote

import club.djipi.lebarcs.domain.model.Game
import club.djipi.lebarcs.domain.model.Move
import club.djipi.lebarcs.domain.model.Player
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.GameStatus
import club.djipi.lebarcs.shared.domain.model.Tile
import club.djipi.lebarcs.shared.dto.GameStateDto
import club.djipi.lebarcs.shared.dto.MoveDto
import club.djipi.lebarcs.shared.dto.PlacedTileDto
import club.djipi.lebarcs.shared.dto.PlayerDto
import club.djipi.lebarcs.shared.dto.TileDto

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