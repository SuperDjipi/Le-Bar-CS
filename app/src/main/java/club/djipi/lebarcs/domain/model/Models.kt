package club.djipi.lebarcs.domain.model

import club.djipi.lebarcs.shared.domain.model.*

/**
 * Mouvement d'un joueur
 */
data class Move(
    val playerId: String,
    val tiles: List<PlacedTile>,     // Tuiles posées ce tour
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlacedTile(
    val tile: Tile,
    val position: Position
)

/**
 * Joueur
 */
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val rack: List<Tile> = emptyList(),  // Chevalet (7 tuiles max)
    val isActive: Boolean = true
)

/**
 * État du jeu
 */
data class Game(
    val id: String,
    val players: List<Player>,
    val board: Board,
    val currentPlayerIndex: Int = 0,
    val tilesRemaining: Int = 102,
    val moves: List<Move> = emptyList(),
    val status: GameStatus = GameStatus.WAITING
)

