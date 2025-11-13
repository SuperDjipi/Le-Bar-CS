package club.djipi.lebarcs.ui.screens.game

import club.djipi.lebarcs.shared.domain.logic.FoundWord
import club.djipi.lebarcs.shared.domain.model.GameState

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        // 1. L'état permanent et officiel du jeu, reçu du serveur.
        val gameData: GameState,

        // 2. L'état temporaire du coup en cours, géré uniquement par l'UI.
        val selectedTileIndex: Int? = null,
        val foundWordsForCurrentMove: List<FoundWord> = emptyList(),
        val currentMoveScore: Int = 0,
        val isCurrentMoveValid: Boolean = false
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}


