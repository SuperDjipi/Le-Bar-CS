package club.djipi.lebarcs.shared.domain.logic

import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.BonusType
import club.djipi.lebarcs.shared.domain.model.BoardPosition

object ScoreCalculator {

    fun calculateScore(word: FoundWord, board: Board, newTiles: List<BoardPosition>): Int {
        var wordScore = 0
        var wordMultiplier = 1

        word.tiles.forEach { (position, tile) ->
            val cell = board.cells[position.row][position.col]
            var tileScore = tile.points
            // On applique les bonus SEULEMENT si la tuile vient d'être posée
            if (position in newTiles) {
                when (cell.bonus) {
                    BonusType.DOUBLE_LETTER -> tileScore *= 2
                    BonusType.TRIPLE_LETTER -> tileScore *= 3
                    BonusType.DOUBLE_WORD -> wordMultiplier *= 2
                    BonusType.CENTER -> wordMultiplier *= 2
                    BonusType.TRIPLE_WORD -> wordMultiplier *= 3
                    else -> { /* No bonus */ }
                }
            }
            wordScore += tileScore
        }

        return wordScore * wordMultiplier
    }

//    fun calculateTotalScoreForMove(allWords: Set<FoundWord>, ...): Int {
//        // ...
//    }
}



