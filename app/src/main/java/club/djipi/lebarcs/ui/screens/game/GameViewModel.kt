package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.shared.domain.logic.*
import club.djipi.lebarcs.shared.domain.model.*
import club.djipi.lebarcs.shared.network.ClientToServerEvent
import club.djipi.lebarcs.shared.network.PlayMovePayload
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// On garde un UiState simple pour la clarté


@HiltViewModel
class GameViewModel @Inject constructor(
    // Les seules dépendances dont on a VRAIMENT besoin
    private val webSocketClient: WebSocketClient,
    private val dictionary: Dictionary
) : ViewModel() {

    private var officialGameState: GameState? = null
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun connectToGame(gameId: String) {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            try {
                // On écoute les messages du serveur.
                webSocketClient.connect(gameId).collect { event ->
                    handleServerEvent(event)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Erreur de connexion", e)
                _uiState.value = GameUiState.Error("Connexion échouée")
            }
        }
    }

    private fun handleServerEvent(event: ServerToClientEvent) {
        Log.d("GameViewModel", "Événement reçu du serveur: $event")
        if (event is ServerToClientEvent.GameStateUpdate) {
            val completeGameState = event.payload.gameState.copy(
                currentPlayerRack = event.payload.playerRack
            )
            // On sauvegarde l'état officiel
            officialGameState = completeGameState
            _uiState.value = GameUiState.Playing(
                gameData = completeGameState,
                localPlayerId = "player1"
                )
            Log.d("GameViewModel", "État mis à jour avec le GameState du serveur.")
        }
    }

    // --- TOUTE LA LOGIQUE DE JEU EST MAINTENANT ICI, CLAIREMENT VISIBLE ---

    fun onTilePlacedFromRack(rackIndex: Int, targetPosition: Position) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        // 1. Préparer les données
        val tileToMove = currentState.gameData.currentPlayerRack.getOrNull(rackIndex) ?: return
        val newRack =
            currentState.gameData.currentPlayerRack.toMutableList().apply { removeAt(rackIndex) }
        val newPlacedTile = PlacedTile(tileToMove, targetPosition)
        val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile
        val newBoard = currentState.gameData.board.withTiles(updatedPlacedTiles)

        // 2. Calculer le nouvel état
        val newState = calculateNewUiState(currentState, newBoard, updatedPlacedTiles, newRack)

        // 3. Mettre à jour l'UI
        _uiState.value = newState
    }

    // Ajoutez ici les autres fonctions d'interaction (onTileMovedOnBoard, onTileReturnedToRack)
    // qui appelleront toutes la même fonction `calculateNewUiState`.

    /**
     * Le "cerveau" de la mise à jour de l'UI. Prend un état et retourne le nouvel état calculé.
     */
    private fun calculateNewUiState(
        currentState: GameUiState.Playing,
        newBoard: Board,
        newPlacedTiles: List<PlacedTile>,
        newRack: List<Tile>
    ): GameUiState.Playing {
        // Validation
        val placedTilesMap = newPlacedTiles.associate { it.position to it.tile }
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
                ScoreCalculator.calculateScore(word, newBoard, newPlacedTiles.map { it.position })
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

    override fun onCleared() {
        super.onCleared()
        webSocketClient.close()
    }
    fun onTileMovedOnBoard(fromPosition: Position, toPosition: Position) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        val tileToMove = currentState.gameData.placedTiles.find { it.position == fromPosition }?.tile ?: return

        val intermediatePlacedTiles = currentState.gameData.placedTiles.filter { it.position != fromPosition }
        val finalPlacedTiles = intermediatePlacedTiles + PlacedTile(tileToMove, toPosition)
        if (officialGameState == null) return
        val newBoard = officialGameState!!.board.withTiles(finalPlacedTiles)

        _uiState.value = calculateNewUiState(
            currentState = currentState,
            newBoard = newBoard,
            newPlacedTiles = finalPlacedTiles,
            newRack = currentState.gameData.currentPlayerRack
        )
    }

    fun onTileReturnedToRack(fromPosition: Position) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        val tileToReturn = currentState.gameData.placedTiles.find { it.position == fromPosition }?.tile ?: return
        val newPlacedTiles = currentState.gameData.placedTiles.filter { it.position != fromPosition }
        val newRack = currentState.gameData.currentPlayerRack + tileToReturn
        if (officialGameState == null) return
        val newBoard = officialGameState!!.board.withTiles(newPlacedTiles)


        _uiState.value = calculateNewUiState(
            currentState = currentState,
            newBoard = newBoard,
            newPlacedTiles = newPlacedTiles,
            newRack = newRack
        )
    }

    fun onRackTilesReordered(fromIndex: Int, toIndex: Int) {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                val newRack = currentState.gameData.currentPlayerRack.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                currentState.copy(gameData = currentState.gameData.copy(currentPlayerRack = newRack))
            } else {
                currentState
            }
        }
    }

    fun onPlayMove() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.gameData.isCurrentMoveValid) {
                // TODO: Envoyer l'événement au serveur via le webSocketClient
                Log.d("GameViewModel", "Bouton JOUER cliqué. Envoi du coup au serveur...")
                try {
                    //1. On crée d'abord le payload
                    val payload = PlayMovePayload(
                        placedTiles= currentState.gameData.placedTiles)
                    // 2. On crée l'événement en lui passant le payload
                    val playMoveEvent = ClientToServerEvent.PlayMove(payload)
                    // 3. (Optionnel) On peut mettre l'UI dans un état d'attente.
                    // Par exemple, en désactivant les boutons en attendant la réponse du serveur.
                    // Cela évite que l'utilisateur ne clique partout.
                    webSocketClient.sendEvent(playMoveEvent)


                } catch (e: Exception) {
                    Log.e("GameViewModel", "Erreur lors de l'envoi du coup", e)
                    // Afficher une erreur à l'utilisateur si l'envoi échoue
                }
            }
        }
    }

    fun onUndoMove() {
        // On vérifie que nous avons bien un état officiel à restaurer.
        if (officialGameState != null) {
            // La seule et unique action : on remplace l'état de l'UI
            // par la dernière version propre que nous avons sauvegardée.
            _uiState.value = GameUiState.Playing(
                gameData = officialGameState!!,
                localPlayerId = "player1"
            )
            Log.d("GameViewModel", "Annulation : retour à l'état officiel du serveur.")
        } else {
            // Ce cas ne devrait pas arriver si on est dans l'état "Playing",
            // mais c'est une sécurité.
            Log.w("GameViewModel", "onUndoMove appelé mais aucun état officiel n'est disponible.")
        }
    }

    fun onShuffleRack() {
        _uiState.update { currentState ->
            if (currentState is GameUiState.Playing) {
                currentState.copy(gameData = currentState.gameData.copy(currentPlayerRack = currentState.gameData.currentPlayerRack.shuffled()))
            } else {
                currentState
            }
        }
    }

    fun onPass() {
        viewModelScope.launch {
            Log.d("GameViewModel", "Le joueur passe son tour. Envoi au serveur...")
            // TODO: Envoyer un événement "PassTurn" au serveur
        }
    }
}