package club.djipi.lebarcs.shared.domain.logic

import  club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Direction
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.shared.domain.model.Tile

// Représente un mot trouvé sur le plateau
data class FoundWord(
    val text: String,
    val tiles: List<Pair<Position, Tile>>, // Pour savoir quelles tuiles le composent
    val direction: Direction
)

object WordFinder {

    /**
     * L'équivalent moderne de votre logique 'isoleMot'.
     * A partir d'une position où une tuile a été posée, trouve le mot horizontal formé.
     */
    fun findHorizontalWord(board: Board, startPos: Position): FoundWord? {
        val row = board.cells[startPos.row]

        // 1. Trouver le début du mot
        var startIndex = startPos.col
        while (startIndex > 0 && row[startIndex - 1].tile != null) {
            startIndex--
        }

        // 2. Trouver la fin du mot
        var endIndex = startPos.col
        while (endIndex < Board.SIZE - 1 && row[endIndex + 1].tile != null) {
            endIndex++
        }

        // 3. Si le mot a une longueur > 1, on le construit
        if (startIndex == endIndex) return null // Mot d'une seule lettre, non connecté

        val wordTiles = mutableListOf<Pair<Position, Tile>>()
        var wordText = ""
        for (col in startIndex..endIndex) {
            val tile = row[col].tile ?: return null // Sécurité
            wordText += tile.letter
            wordTiles.add(Position(startPos.row, col) to tile)
        }

        return FoundWord(wordText, wordTiles, Direction.HORIZONTAL)
    }

    // On créera une fonction similaire `findVerticalWord(...)`
    fun findVerticalWord(board: Board, startPos: Position): FoundWord? { return null/* ... */ }


    /**
     * La fonction principale qui, pour un coup (un ensemble de tuiles posées),
     * trouve TOUS les mots formés.
     */
    fun findAllWordsFormedByMove(board: Board, placedTiles: Map<Position, Tile>): Set<FoundWord> {
        val allWords = mutableSetOf<FoundWord>()
        if (placedTiles.isEmpty()) return emptySet()

        // Logique pour déterminer la direction principale du coup (horizontal ou vertical)
        // ...

        // Pour chaque tuile posée...
        placedTiles.keys.forEach { pos ->
            // ... on trouve le mot horizontal et le mot vertical qui la traversent
            findHorizontalWord(board, pos)?.let { allWords.add(it) }
            findVerticalWord(board, pos)?.let { allWords.add(it) }
        }

        return allWords
    }
}



