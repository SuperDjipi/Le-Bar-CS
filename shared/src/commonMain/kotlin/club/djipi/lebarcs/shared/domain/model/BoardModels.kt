package club.djipi.lebarcs.shared.domain.model


import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
/**
 * Position sur le plateau (0-14 pour un plateau 15x15)
 */
@Immutable
@Serializable
data class BoardPosition(
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
@Immutable
@Serializable
data class BoardCell(
    val boardPosition: BoardPosition,
    val bonus: BonusType = BonusType.NONE,
    val tile: Tile? = null,        // null si case vide
    val isLocked: Boolean = false   // true si la tuile a déjà été jouée
)

/**
 * Plateau de jeu 15x15
 */
@Immutable
@Serializable
@JvmInline
value class Board(
    val cells: List<List<BoardCell>> = initializeBoard()
) {
    fun getTile(boardPosition: BoardPosition): Tile? {
        return cells[boardPosition.row][boardPosition.col].tile
    }

    /**
     * Extension pour vérifier si le plateau est vide (aucune tuile posée)
     */
    fun isEmpty(): Boolean {
        return cells.all { row -> row.all { cell -> cell.tile == null } }
    }

    /**
     * Extension pour récupérer une cellule à une position donnée
     */
    fun getCellAt(position: BoardPosition): BoardCell? {
        return if (position.row in 0 until SIZE && position.col in 0 until SIZE) {
            cells[position.row][position.col]
        } else {
            null
        }
    }

    /**
     * Extension pour vérifier si une position contient une tuile
     */
    fun hasTileAt(position: BoardPosition): Boolean {
        return getCellAt(position)?.tile != null
    }

    companion object {
        const val SIZE = 15
        // On crée une fonction 'invoke' pour pouvoir faire Board() comme avant.
        operator fun invoke(): Board {
            return Board(initializeBoard())
        }
        private fun initializeBoard(): List<List<BoardCell>> {
            return List(SIZE) { row ->
                List(SIZE) { col ->
                    BoardCell(
                        boardPosition = BoardPosition(row, col),
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
 * Fonction d'extension pour créer une nouvelle version du plateau
 * avec une liste de tuiles posées.
 * C'est une fonction "pure", elle ne modifie pas le plateau original.
 */
fun Board.withTiles(placedTiles: List<PlacedTile>): Board {
    // 1. On crée une copie mutable de la grille de cellules actuelle.
    val newCells = this.cells.map { row -> row.toMutableList() }.toMutableList()

    // 2. Pour chaque tuile à placer, on met à jour la cellule correspondante.
    placedTiles.forEach { (tile, position) ->
        // On s'assure de ne pas écrire en dehors du plateau
        if (position.row in 0..<Board.SIZE && position.col in 0..<Board.SIZE) {
            val currentCell = newCells[position.row][position.col]
            // On crée une nouvelle cellule avec la tuile mise à jour
            newCells[position.row][position.col] = currentCell.copy(tile = tile)
        }
    }

    // 3. On crée et retourne un nouvel objet Board avec la grille de cellules mise à jour.
    return Board(newCells)
}