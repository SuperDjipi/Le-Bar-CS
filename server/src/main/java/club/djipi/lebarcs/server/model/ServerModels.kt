package club.djipi.lebarcs.server.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ServerGame(
    val id: String,
    val hostPlayerId: String,
    val players: MutableList<ServerPlayer> = mutableListOf(),
    val status: String = "WAITING",
    val tileBag: TileBag = TileBag(),
    val currentPlayerIndex: Int = 0
) {
    fun toGameStateDto(): GameStateDto {
        return GameStateDto(
            gameId = id,
            players = players.map { it.toDto() },
            currentPlayerIndex = currentPlayerIndex,
            tilesRemaining = tileBag.remainingTiles(),
            status = status,
            board = emptyList() // À implémenter
        )
    }
}

@Serializable
data class ServerPlayer(
    val id: String,
    val name: String,
    val score: Int = 0,
    val rack: MutableList<ServerTile> = mutableListOf(),
    val isActive: Boolean = true
) {
    fun toDto(): PlayerDto {
        return PlayerDto(
            id = id,
            name = name,
            score = score,
            rackSize = rack.size,
            isActive = isActive
        )
    }
}

@Serializable
data class ServerTile(
    val letter: Char,
    val points: Int,
    val isJoker: Boolean = false
)

/**
 * Sac de tuiles
 */
class TileBag {
    private val tiles = mutableListOf<ServerTile>()
    
    init {
        initializeTiles()
    }
    
    private fun initializeTiles() {
        // Distribution du Scrabble français
        val distribution = mapOf(
            'A' to 9, 'B' to 2, 'C' to 2, 'D' to 3, 'E' to 15,
            'F' to 2, 'G' to 2, 'H' to 2, 'I' to 8, 'J' to 1,
            'K' to 1, 'L' to 5, 'M' to 3, 'N' to 6, 'O' to 6,
            'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 6, 'T' to 6,
            'U' to 6, 'V' to 2, 'W' to 1, 'X' to 1, 'Y' to 1,
            'Z' to 1
        )
        
        val points = mapOf(
            'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1,
            'F' to 4, 'G' to 2, 'H' to 4, 'I' to 1, 'J' to 8,
            'K' to 10, 'L' to 1, 'M' to 2, 'N' to 1, 'O' to 1,
            'P' to 3, 'Q' to 8, 'R' to 1, 'S' to 1, 'T' to 1,
            'U' to 1, 'V' to 4, 'W' to 10, 'X' to 10, 'Y' to 10,
            'Z' to 10
        )
        
        distribution.forEach { (letter, count) ->
            repeat(count) {
                tiles.add(ServerTile(letter, points[letter] ?: 0))
            }
        }
        
        // Ajouter les 2 jokers
        repeat(2) {
            tiles.add(ServerTile('_', 0, isJoker = true))
        }
        
        tiles.shuffle()
    }
    
    fun drawTiles(count: Int): List<ServerTile> {
        val drawn = tiles.take(count)
        repeat(drawn.size) { tiles.removeAt(0) }
        return drawn
    }
    
    fun remainingTiles(): Int = tiles.size
    
    fun returnTiles(tilesToReturn: List<ServerTile>) {
        tiles.addAll(tilesToReturn)
        tiles.shuffle()
    }
}

// DTOs pour la sérialisation
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
    val rackSize: Int,
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
