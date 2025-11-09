package club.djipi.lebarcs.ui.screens.game

import club.djipi.lebarcs.shared.domain.model.GameState

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val gameData: GameState,
        val selectedTileIndex: Int? = null
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}


