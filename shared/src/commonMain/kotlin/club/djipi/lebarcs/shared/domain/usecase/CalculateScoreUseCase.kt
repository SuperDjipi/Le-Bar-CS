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
        // On utilise la fonction 'sumOf' qui est plus concise et idiomatique en Kotlin.
        val wordsScore = foundWords.sumOf { word ->
            ScoreCalculator.calculateScore(word, newBoard, newlyPlacedPositions)
        }
        // On gère le bonus si le joueur a posé 7 tuiles.
        val scrabbleBonus = if (newlyPlacedPositions.size == 7) 50 else 0
        // --- FIN DE LA FINALISATION ---

        return wordsScore + scrabbleBonus
    }
}