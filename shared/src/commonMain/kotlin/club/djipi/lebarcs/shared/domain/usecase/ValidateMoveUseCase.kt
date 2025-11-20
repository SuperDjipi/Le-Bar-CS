package club.djipi.lebarcs.shared.domain.usecase

import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.shared.domain.logic.MoveValidator
import club.djipi.lebarcs.shared.domain.logic.WordFinder
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.PlacedTile


class ValidateMoveUseCase(
    private val dictionary: Dictionary
) {
    operator fun invoke(
        originalBoard: Board,
        newBoard: Board,
        placedTiles: List<PlacedTile>,
        turnNumber: Int
    ): Boolean {
        if (placedTiles.isEmpty()) return false

        val placedTilesMap = placedTiles.associate { it.boardPosition to it.tile }

        val isPlacementValid = MoveValidator.isPlacementValid(originalBoard, placedTilesMap.keys)
        val isMoveConnected =
            MoveValidator.isMoveConnected(originalBoard, placedTilesMap.keys, turnNumber)
        val foundWords = WordFinder.findAllWordsFormedByMove(newBoard, placedTilesMap)
        val allWordsAreInDico =
            foundWords.isNotEmpty() && foundWords.all { dictionary.isValid(it.text) }

        return isPlacementValid && isMoveConnected && allWordsAreInDico
    }
}