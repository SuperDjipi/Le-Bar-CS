package club.djipi.lebarcs.shared.domain.model


/**
 * Position sur le plateau (0-14 pour un plateau 15x15)
 */
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
    CENTER          // Case centrale (étoile) Mot compte double
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