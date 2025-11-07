package club.djipi.lebarcs.ui.screens.game

import club.djipi.lebarcs.domain.model.PlacedTile
import club.djipi.lebarcs.domain.model.Player
import club.djipi.lebarcs.shared.domain.logic.FoundWord
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Tile


sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val gameData: GameData,
        val selectedTileIndex: Int? = null
    ) : GameUiState()
    data class Error(val message: String) : GameUiState()
}

data class GameData(
    val players: List<Player>,
    val board: Board,
    val currentPlayerIndex: Int,
    val currentPlayerRack: List<Tile>,
    val placedTiles: List<PlacedTile> = emptyList(),
    val foundWords: List<FoundWord> = emptyList(),
    val currentMoveScore: Int = 0,
    val isPlacementValid: Boolean = true, // par défaut, un coup est valide en géométrie
    val areWordsValid: Boolean = true,      // Par défaut, il n'y a pas de mots invalides
    val isCurrentMoveValid: Boolean = false // Par défaut, un coup vide n'est pas jouable
) {
    val hasPlacedTiles: Boolean
        get() = placedTiles.isNotEmpty()
}
