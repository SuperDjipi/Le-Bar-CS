package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.shared.domain.logic.*
import club.djipi.lebarcs.shared.domain.model.*
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val dictionary: Dictionary,
    private val gameRepository: GameRepository
) : ViewModel() {

    private var localPlayerId: String? = null
    private var officialGameState: GameState? = null
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // --- GESTION DE L'IDENTITÉ ET DE LA CONNEXION ---
    fun setLocalPlayerId(playerId: String) {
        if (this.localPlayerId == null) {
            this.localPlayerId = playerId
            Log.d("GameViewModel", "ID joueur local défini : $playerId")
        }
    }

    fun connectToGame(gameId: String) {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            val playerId = localPlayerId ?: run { /* ... gestion d'erreur ... */ return@launch }
            try {
                // Le ViewModel ne fait que déléguer. C'est le Repository qui gère la connexion.
                gameRepository.connect(gameId, playerId)
                // On écoute les événements via le Repository
                gameRepository.getEvents().collect { event ->
                    handleServerEvent(event)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Erreur de connexion", e)
                _uiState.value = GameUiState.Error("Connexion échouée")
            }
        }
    }

    private fun calculateNewUiState(
        currentState: GameUiState.Playing,
        newBoard: Board,
        newPlacedTiles: List<PlacedTile>,
        newRack: List<Tile>
    ): GameUiState.Playing {
        // Validation
        val placedTilesMap = newPlacedTiles.associate { it.boardPosition to it.tile }
        val isPlacementValid =
            MoveValidator.isPlacementValid(currentState.gameData.board, placedTilesMap.keys)
        val isMoveConnected = MoveValidator.isMoveConnected(
            currentState.gameData.board,
            placedTilesMap.keys,
            currentState.gameData.turnNumber
        )
        val foundWords = WordFinder.findAllWordsFormedByMove(newBoard, placedTilesMap)
        val allWordsAreInDico =
            foundWords.isNotEmpty() && foundWords.all { dictionary.isValid(it.text) }

        val isMoveValid = isPlacementValid && isMoveConnected && allWordsAreInDico

        Log.d(
            "GameViewModel_Logic",
            "Validation: Placement=$isPlacementValid, Connexion=$isMoveConnected, Dico=$allWordsAreInDico -> Final=$isMoveValid"
        )

        // Calcul du score
        val score = if (isMoveValid) {
            val scrabbleBonus = if (newPlacedTiles.size == 7) 50 else 0
            foundWords.sumOf { word ->
                ScoreCalculator.calculateScore(
                    word,
                    newBoard,
                    newPlacedTiles.map { it.boardPosition })
            } + scrabbleBonus
        } else {
            0
        }

        Log.d("GameViewModel_Logic", "Mots trouvés: $foundWords, Score calculé: $score")

        // Retourner le nouvel état complet
        return currentState.copy(
            gameData = currentState.gameData.copy(
                board = newBoard,
                currentPlayerRack = newRack,
                placedTiles = newPlacedTiles,
                currentMoveScore = score,
                isCurrentMoveValid = isMoveValid
            )
        )
    }


    private fun handleServerEvent(event: ServerToClientEvent) {
        when (event) {
            is ServerToClientEvent.GameStateUpdate -> {
                Log.d("GameViewModel", "Événement reçu du serveur : $event")

                // --- DÉBUT DE LA CORRECTION ---
                // Le 'newGameState' du serveur a des chevalets vides.
                val publicGameState = event.payload.gameState
                // Le 'playerRack' contient le chevalet privé du joueur local.
                val localPlayerRack = event.payload.playerRack

                // 1. On construit l'état COMPLET que l'on veut utiliser et sauvegarder.
                //    C'est l'état public, mais on y ré-insère notre chevalet privé.
                val completeGameState = publicGameState.copy(
                    currentPlayerRack = localPlayerRack
                )

                // 2. On sauvegarde CET état complet comme notre point de restauration.
                officialGameState = completeGameState

                // 3. On met à jour l'UI avec ce même état complet.
                _uiState.value = GameUiState.Playing(
                    gameData = completeGameState,
                    localPlayerId = localPlayerId ?: ""
                )
                // --- FIN DE LA CORRECTION ---

                Log.d("GameViewModel", "État mis à jour avec le GameState du serveur.")
            }
            is ServerToClientEvent.ErrorMessage -> {
                Log.e("GameViewModel", "Erreur reçue du serveur : ${event.payload.message}")
//                _uiState.update {
//                    if (it is GameUiState.Playing) it.copy(error = event.payload.message)
//                    else GameUiState.Error(event.payload.message)
//                }
            }
            // ... autres 'when' si nécessaire
        }
    }


    // --- Utilitaires UX
    /**
     * Calcule les positions valides pour placer une tuile.
     * Doit retourner les cases adjacentes aux mots existants + la case centrale si vide.
     */
    fun getValidDropPositions(fromRack: Boolean = true): Set<BoardPosition> {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return emptySet()

        val board = currentState.gameData.board
        val validPositions = mutableSetOf<BoardPosition>()

        // Si c'est le premier coup, seulement la case centrale
        if (board.isEmpty()) {
            validPositions.add(BoardPosition(7, 7)) // Case centrale (15x15)
            return validPositions
        }
        // Sinon, toutes les cases vides adjacentes à une tuile existante
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = BoardPosition(row, col)
                val cell = board.getCellAt(pos)

                // Si vide ET adjacente à une tuile
                if (cell?.tile == null && hasAdjacentTile(board, pos)) {
                    validPositions.add(pos)
                }
            }
        }
        return validPositions
    }

    private fun hasAdjacentTile(board: Board, position: BoardPosition): Boolean {
        val adjacents = listOf(
            BoardPosition(position.row - 1, position.col),
            BoardPosition(position.row + 1, position.col),
            BoardPosition(position.row, position.col - 1),
            BoardPosition(position.row, position.col + 1)
        )
        return adjacents.any { board.getCellAt(it)?.tile != null }
    }


    // --- ACTIONS DU LOBBY ET DU JEU ---
    fun onStartGame() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.isLocalPlayerHost) {
                try {
                    gameRepository.sendStartGame()
                    Log.d("GameViewModel", "Demande START_GAME envoyée au repository.")
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Erreur lors de l'envoi de START_GAME", e)
                }
            }
        }
    }

    fun onPlayMove() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.gameData.isCurrentMoveValid) {
                try {
                    // On délègue au Repository
                    gameRepository.sendPlayMove(currentState.gameData.placedTiles)
                    Log.d("GameViewModel", "Demande PLAY_MOVE envoyée au repository.")
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Erreur lors de l'envoi du coup", e)
                }
            }
        }
    }

    fun onPassTurn() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.isLocalPlayerTurn) {
                try {
                    // On délègue au Repository
                    gameRepository.sendPassTurn()
                    Log.d("GameViewModel", "Demande PASS_TURN envoyée au repository.")
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Erreur lors de l'envoi de PASS_TURN", e)
                }
            }
        }
    }

    fun onTilePlacedFromRack(rackIndex: Int, targetPosition: BoardPosition) {
        val currentState = _uiState.value as? GameUiState.Playing ?: return
        val tileToMove = currentState.gameData.currentPlayerRack.getOrNull(rackIndex) ?: return

        // Logique pour le joker
        if (tileToMove.isJoker) {
            _uiState.update {
                (it as GameUiState.Playing).copy(
                    jokerSelectionState = GameUiState.JokerSelectionState.Selecting(
                        targetPosition,
                        tileToMove.id
                    )
                )
            }
            return
        }

        val newRack =
            currentState.gameData.currentPlayerRack.toMutableList().apply { removeAt(rackIndex) }
        val newPlacedTile = PlacedTile(tileToMove, targetPosition)
        val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile
        val newBoard = officialGameState!!.board.withTiles(updatedPlacedTiles)

        _uiState.value = calculateNewUiState(currentState, newBoard, updatedPlacedTiles, newRack)
    }

    fun onTileMovedOnBoard(fromPosition: BoardPosition, toPosition: BoardPosition) {
        val currentState = _uiState.value as? GameUiState.Playing ?: return
        val tileToMove =
            currentState.gameData.placedTiles.find { it.boardPosition == fromPosition }?.tile
                ?: return

        val intermediatePlacedTiles =
            currentState.gameData.placedTiles.filter { it.boardPosition != fromPosition }
        val finalPlacedTiles = intermediatePlacedTiles + PlacedTile(tileToMove, toPosition)
        val newBoard = officialGameState!!.board.withTiles(finalPlacedTiles)

        _uiState.value = calculateNewUiState(
            currentState,
            newBoard,
            finalPlacedTiles,
            currentState.gameData.currentPlayerRack
        )
    }

    fun onTileReturnedToRack(fromPosition: BoardPosition) {
        val currentState = _uiState.value as? GameUiState.Playing ?: return
        val tileToReturn =
            currentState.gameData.placedTiles.find { it.boardPosition == fromPosition }?.tile
                ?: return

        val newPlacedTiles =
            currentState.gameData.placedTiles.filter { it.boardPosition != fromPosition }
        val newRack =
            (currentState.gameData.currentPlayerRack + tileToReturn).sortedBy { it.letter }
        val newBoard = officialGameState!!.board.withTiles(newPlacedTiles)

        _uiState.value = calculateNewUiState(currentState, newBoard, newPlacedTiles, newRack)
    }

    fun onRackTilesReordered(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? GameUiState.Playing ?: return
        val currentRack = currentState.gameData.currentPlayerRack
        if (fromIndex !in currentRack.indices || toIndex !in currentRack.indices) return

        val reorderedRack = currentRack.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        _uiState.update {
            (it as GameUiState.Playing).copy(
                gameData = it.gameData.copy(
                    currentPlayerRack = reorderedRack
                )
            )
        }
    }

    fun onUndoMove() {
        if (officialGameState != null) {
            _uiState.value = GameUiState.Playing(
                gameData = officialGameState!!,
                localPlayerId = localPlayerId ?: ""
            )
        }
    }

    fun onShuffleRack() {
        val currentState = _uiState.value as? GameUiState.Playing ?: return
        _uiState.update {
            (it as GameUiState.Playing).copy(
                gameData = it.gameData.copy(
                    currentPlayerRack = it.gameData.currentPlayerRack.shuffled()
                )
            )
        }
    }

    /**
     * Nettoie les ressources (la connexion WebSocket) lorsque le ViewModel est détruit.
     */
    override fun onCleared() {
        super.onCleared()
        // On délègue la fermeture au Repository
        gameRepository.close()
        Log.d("GameViewModel", "ViewModel détruit, connexion fermée.")
    }

    /**
     * Appelé quand l'utilisateur sélectionne une lettre pour le joker.
     * Met à jour la tuile avec la lettre choisie et ferme le dialog.
     */
    fun onJokerLetterSelected(letter: Char) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        val selectionState = currentState.jokerSelectionState
        if (selectionState !is GameUiState.JokerSelectionState.Selecting) return

        // 1. On retrouve la tuile joker originale sur le chevalet grâce à son ID.
        val jokerRackIndex =
            currentState.gameData.currentPlayerRack.indexOfFirst { it.id == selectionState.tileId }
        if (jokerRackIndex == -1) {
            Log.e(
                "GameViewModel",
                "Impossible de retrouver le joker avec l'ID ${selectionState.tileId} sur le chevalet."
            )
            return
        }
        val jokerTile = currentState.gameData.currentPlayerRack[jokerRackIndex]

        // 2. On crée la nouvelle tuile joker avec la lettre assignée.
        val assignedJoker = jokerTile.copy(assignedLetter = letter.uppercase())

        // 3. On simule le placement de cette nouvelle tuile.
        //    a. On retire la tuile originale du chevalet.
        val newRack = currentState.gameData.currentPlayerRack.toMutableList()
            .apply { removeAt(jokerRackIndex) }
        //    b. On crée le 'PlacedTile' avec la tuile assignée et la position cible.
        val newPlacedTile = PlacedTile(assignedJoker, selectionState.targetBoardPosition)
        //    c. On met à jour la liste des tuiles posées pendant ce tour.
        val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile
        //    d. On crée le nouveau plateau visuel.
        val newBoard = officialGameState!!.board.withTiles(updatedPlacedTiles)

        // 4. On appelle notre "cerveau" pour qu'il recalcule tout.
        //    Il va valider le coup, calculer le score, etc.
        val newState = calculateNewUiState(currentState, newBoard, updatedPlacedTiles, newRack)

        // 5. On applique ce nouvel état et on ferme le dialogue en une seule opération.
        _uiState.value = newState.copy(
            jokerSelectionState = null  // <-- C'est ici qu'on ferme le dialogue !
        )

        Log.d(
            "GameViewModel",
            "Joker assigné à la lettre '$letter' et placé sur ${selectionState.targetBoardPosition}."
        )
    }
}