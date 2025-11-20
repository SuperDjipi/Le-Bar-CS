package club.djipi.lebarcs.shared.domain.usecase

import club.djipi.lebarcs.shared.domain.logic.FoundWord
import club.djipi.lebarcs.shared.domain.logic.ScoreCalculator
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.BoardPosition


class CalculateScoreUseCase {
    operator fun invoke(
        foundWords: Set<FoundWord>,
        newBoard: Board,
        newlyPlacedBoardPositions: List<BoardPosition>
    ): Int {
        if (foundWords.isEmpty()) return 0
        // On utilise la fonction 'sumOf' qui est plus concise et idiomatique en Kotlin.
        val wordsScore = foundWords.sumOf { word ->
            ScoreCalculator.calculateScore(word, newBoard, newlyPlacedBoardPositions)
        }
        // On gère le bonus si le joueur a posé 7 tuiles.
        val scrabbleBonus = if (newlyPlacedBoardPositions.size == 7) 50 else 0
        // --- FIN DE LA FINALISATION ---

        return wordsScore + scrabbleBonus
    }
}