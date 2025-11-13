package club.djipi.lebarcs.shared.domain.usecase

import club.djipi.lebarcs.shared.domain.logic.FoundWord
import club.djipi.lebarcs.shared.domain.logic.ScoreCalculator
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Position


class CalculateScoreUseCase {
    operator fun invoke(
        foundWords: Set<FoundWord>,
        newBoard: Board,
        newlyPlacedPositions: List<Position>
    ): Int {
        if (foundWords.isEmpty()) return 0

        var totalScore = 0
        foundWords.forEach { word ->
            totalScore += ScoreCalculator.calculateScore(word, newBoard, newlyPlacedPositions)
        }

        // TODO: Gérer le bonus de 50 points si 7 tuiles sont posées

        return totalScore
    }
}