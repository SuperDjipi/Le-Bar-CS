package club.djipi.lebarcs.domain.model

import kotlinx.serialization.Serializable

/**
 * Position sur le plateau (0-14 pour un plateau 15x15)
 */
@Serializable
data class Position(
    val row: Int,
    val col: Int
)

/**
 * Type de case bonus
 */
enum class BonusType {
    NONE,           // Case normale
    DOUBLE_LETTER,  // Lettre compte double
    TRIPLE_LETTER,  // Lettre compte triple
    DOUBLE_WORD,    // Mot compte double
    TRIPLE_WORD,    // Mot compte triple
    CENTER          // Case centrale (étoile)
}

/**
 * Tuile du jeu
 */
@Serializable
data class Tile(
    val letter: Char,       // 'A'-'Z' ou '_' pour joker
    val points: Int,        // Valeur en points
    val isJoker: Boolean = false
) {
    companion object {
        // Valeurs standard du Scrabble français
        val TILE_VALUES = mapOf(
            'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1,
            'F' to 4, 'G' to 2, 'H' to 4, 'I' to 1, 'J' to 8,
            'K' to 10, 'L' to 1, 'M' to 2, 'N' to 1, 'O' to 1,
            'P' to 3, 'Q' to 8, 'R' to 1, 'S' to 1, 'T' to 1,
            'U' to 1, 'V' to 4, 'W' to 10, 'X' to 10, 'Y' to 10,
            'Z' to 10, '_' to 0
        )
        
        // Distribution des tuiles en français
        val TILE_DISTRIBUTION = mapOf(
            'A' to 9, 'B' to 2, 'C' to 2, 'D' to 3, 'E' to 15,
            'F' to 2, 'G' to 2, 'H' to 2, 'I' to 8, 'J' to 1,
            'K' to 1, 'L' to 5, 'M' to 3, 'N' to 6, 'O' to 6,
            'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 6, 'T' to 6,
            'U' to 6, 'V' to 2, 'W' to 1, 'X' to 1, 'Y' to 1,
            'Z' to 1, '_' to 2 // Jokers
        )
    }
}

/**
 * Case du plateau
 */
data class BoardCell(
    val position: Position,
    val bonus: BonusType = BonusType.NONE,
    val tile: Tile? = null,        // null si case vide
    val isLocked: Boolean = false   // true si la tuile a déjà été jouée
)

/**
 * Plateau de jeu 15x15
 */
data class Board(
    val cells: List<List<BoardCell>> = initializeBoard()
) {
    fun getTile(position: Position): Tile? {
        return cells[position.row][position.col].tile
    }
    
    fun getCell(position: Position): BoardCell {
        return cells[position.row][position.col]
    }
    
    companion object {
        const val SIZE = 15
        
        private fun initializeBoard(): List<List<BoardCell>> {
            return List(SIZE) { row ->
                List(SIZE) { col ->
                    BoardCell(
                        position = Position(row, col),
                        bonus = determineBonusType(row, col)
                    )
                }
            }
        }
        
        private fun determineBonusType(row: Int, col: Int): BonusType {
            // Configuration standard du plateau de Scrabble
            return when {
                row == 7 && col == 7 -> BonusType.CENTER
                
                // Mot compte triple (coins)
                (row == 0 || row == 14) && (col == 0 || col == 7 || col == 14) -> BonusType.TRIPLE_WORD
                (row == 7) && (col == 0 || col == 14) -> BonusType.TRIPLE_WORD
                
                // Mot compte double
                (row == col || row + col == 14) && row in 1..13 -> BonusType.DOUBLE_WORD
                
                // Lettre compte triple
                (row == 1 || row == 13) && (col == 5 || col == 9) -> BonusType.TRIPLE_LETTER
                (row == 5 || row == 9) && (col == 1 || col == 5 || col == 9 || col == 13) -> BonusType.TRIPLE_LETTER
                
                // Lettre compte double
                (row == 0 || row == 14) && (col == 3 || col == 11) -> BonusType.DOUBLE_LETTER
                (row == 2 || row == 12) && (col == 6 || col == 8) -> BonusType.DOUBLE_LETTER
                (row == 3 || row == 11) && (col == 0 || col == 7 || col == 14) -> BonusType.DOUBLE_LETTER
                (row == 6 || row == 8) && (col == 2 || col == 6 || col == 8 || col == 12) -> BonusType.DOUBLE_LETTER
                (row == 7) && (col == 3 || col == 11) -> BonusType.DOUBLE_LETTER
                
                else -> BonusType.NONE
            }
        }
    }
}

/**
 * Mouvement d'un joueur
 */
@Serializable
data class Move(
    val playerId: String,
    val tiles: List<PlacedTile>,     // Tuiles posées ce tour
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PlacedTile(
    val tile: Tile,
    val position: Position
)

/**
 * Joueur
 */
@Serializable
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
@Serializable
data class Game(
    val id: String,
    val players: List<Player>,
    val board: Board,
    val currentPlayerIndex: Int = 0,
    val tilesRemaining: Int = 102,
    val moves: List<Move> = emptyList(),
    val status: GameStatus = GameStatus.WAITING
)

enum class GameStatus {
    WAITING,    // En attente de joueurs
    PLAYING,    // Partie en cours
    FINISHED    // Partie terminée
}

/**
 * Direction du placement des tuiles
 */
enum class Direction {
    HORIZONTAL,
    VERTICAL
}
