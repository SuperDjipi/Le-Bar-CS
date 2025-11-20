package club.djipi.lebarcs.shared.domain.logic

import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Direction
import club.djipi.lebarcs.shared.domain.model.BoardPosition
import club.djipi.lebarcs.shared.domain.model.Tile
import kotlinx.serialization.Serializable

// Représente un mot trouvé sur le plateau (ne change pas)
@Serializable
data class FoundWord(
    val text: String,
    val tiles: List<Pair<BoardPosition, Tile>>, // Pour savoir quelles tuiles le composent
    val direction: Direction
)

object WordFinder {

    // --- ON RECOMMENCE ICI, PROPREMENT ---
    fun findHorizontalWord(board: Board, startPos: BoardPosition): FoundWord? {
        // Sécurité : on s'assure que la position de départ est valide
        if (board.getTile(startPos) == null) {
            return null
        }

        val boardSize = Board.SIZE
        val row = startPos.row

        // 1. Trouver la colonne de début
        var startIndex = startPos.col
        while (startIndex > 0 && board.getTile(BoardPosition(row, startIndex - 1)) != null) {
            startIndex--
        }

        // 2. Trouver la colonne de fin
        var endIndex = startPos.col
        while (endIndex < boardSize - 1 && board.getTile(BoardPosition(row, endIndex + 1)) != null) {
            endIndex++
        }

        // Si le mot ne fait qu'une lettre, on l'ignore (un mot doit être formé par au moins 2 lettres)
        if (startIndex == endIndex) {
            return null
        }

        // 3. Construire le mot
        val wordTiles = mutableListOf<Pair<BoardPosition, Tile>>()
        var wordText = ""
        for (col in startIndex..endIndex) {
            val currentPos = BoardPosition(row, col)
            val tile = board.getTile(currentPos) ?: return null // Sécurité, ne devrait jamais arriver
            wordText += tile.displayLetter
            wordTiles.add(currentPos to tile)
        }

        return FoundWord(wordText, wordTiles, Direction.HORIZONTAL)
    }

    fun findVerticalWord(board: Board, startPos: BoardPosition): FoundWord? {
        if (board.getTile(startPos) == null) {
            return null
        }

        val boardSize = Board.SIZE
        val col = startPos.col

        // 1. Trouver la ligne de début (en remontant)
        var startRow = startPos.row
        while (startRow > 0 && board.getTile(BoardPosition(startRow - 1, col)) != null) {
            startRow--
        }

        // 2. Trouver la ligne de fin (en descendant)
        var endRow = startPos.row
        while (endRow < boardSize - 1 && board.getTile(BoardPosition(endRow + 1, col)) != null) {
            endRow++
        }

        if (startRow == endRow) {
            return null
        }

        // 3. Construire le mot
        val wordTiles = mutableListOf<Pair<BoardPosition, Tile>>()
        var wordText = ""
        for (row in startRow..endRow) {
            val currentPos = BoardPosition(row, col)
            val tile = board.getTile(currentPos) ?: return null
            wordText += tile.displayLetter
            wordTiles.add(currentPos to tile)
        }

        return FoundWord(wordText, wordTiles, Direction.VERTICAL)
    }

    fun findAllWordsFormedByMove(board: Board, placedTiles: Map<BoardPosition, Tile>): Set<FoundWord> {
        if (placedTiles.isEmpty()) return emptySet()

        val allFoundWords = mutableSetOf<FoundWord>()

        // Pour chaque tuile qui vient d'être posée...
        placedTiles.keys.forEach { position ->
            // ...on cherche le mot horizontal qui la contient...
            findHorizontalWord(board, position)?.let { allFoundWords.add(it) }
            // ...et on cherche le mot vertical qui la contient.
            findVerticalWord(board, position)?.let { allFoundWords.add(it) }
        }

        // Cette logique est une sécurité supplémentaire si un mot d'une seule lettre a été joué,
        // mais qu'il est connecté à d'autres, formant un mot plus long.
        // Si aucun mot n'est trouvé alors qu'on a posé des tuiles, c'est qu'on a peut-être formé
        // un mot à partir d'une seule tuile posée. La boucle ci-dessus suffit normalement.
        // On peut simplifier : si la boucle trouve des mots, c'est bon.

        return allFoundWords
    }
}
