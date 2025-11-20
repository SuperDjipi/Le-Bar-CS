package club.djipi.lebarcs.ui.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.djipi.lebarcs.data.local.UserPreferencesRepository
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

/**
 * Le ViewModel partagé pour tout le "flux de jeu" (Lobby et GameScreen).
 *  *
 *  * Il agit comme un cerveau central qui :
 *  * 1.  Gère la connexion WebSocket avec le serveur.
 *  * 2.  Maintient l'état actuel de la partie (`GameState`).
 *  * 3.  Traite les actions de l'utilisateur (poser une tuile, jouer un
 * coup, etc.).
 *  * 4.  Calcule la validité et le score des coups en temps réel.
 *  * 5.  Expose un unique état (`GameUiState`) que l'interface utilisateur observe pour se mettre à jour.
 *  *
 *  * Son cycle de vie est lié au graphe de navigation "game_flow" grâce à
 * Hilt,
 *  * garantissant sa persistance entre le LobbyScreen et le GameScreen.
 *  */
@HiltViewModel
class GameViewModel @Inject constructor(
    // Les seules dépendances dont on a VRAIMENT besoin
    private val webSocketClient: WebSocketClient,
    private val dictionary: Dictionary,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // --- PROPRIÉTÉS D'ÉTAT ---

    /** L'ID persistant du joueur local, reçu une seule fois au début. */
    private var localPlayerId: String? = null

    /** Une copie du dernier état de jeu officiel reçu du serveur. Essentiel pour la fonction "Annuler". */
    private var officialGameState: GameState? = null

    /** Le StateFlow privé et mutable qui contient l'état actuel de l'UI. */
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)

    /** Le StateFlow public et immuable, exposé à l'UI pour observation. */
    val uiState = _uiState.asStateFlow()

    // --- GESTION DE L'IDENTITÉ ET DE LA CONNEXION ---

    /**
     * Méthode appelée par l'UI (via le NavGraph) pour informer le ViewModel de l'identité du joueur local.
     * C'est la première étape avant toute connexion.
     */
    fun setLocalPlayerId(playerId: String) {
        if (this.localPlayerId == null) { // Sécurité pour ne le définir qu'une seule fois.
            this.localPlayerId = playerId
            Log.d("GameViewModel", "ID joueur local défini : $playerId")
        }
    }

    fun onStartGame() {
        viewModelScope.launch {
            // Sécurité : on vérifie que c'est bien le tour de l'hôte, etc.
            val currentState = uiState.value
            if (currentState is GameUiState.Playing && currentState.isLocalPlayerHost) {
                try {
                    // On envoie l'objet 'StartGame' directement via le WebSocket.
                    webSocketClient.sendEvent(ClientToServerEvent.StartGame)
                    Log.d("GameViewModel", "Événement START_GAME envoyé au serveur.")
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Erreur lors de l'envoi de START_GAME", e)
                }
            }
        }
    }
    /**
     * Lance le processus de connexion au serveur de jeu via WebSocket.
     * Appelée par le `LobbyScreen`.
     */
    fun connectToGame(gameId: String) {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            // Sécurité : On s'assure que l'ID du joueur est bien défini avant de continuer.
            val playerId = localPlayerId ?: run {
                Log.e("GameViewModel", "setLocalPlayerId n'a pas été appelé avant connectToGame.")
                return@launch
            }
            try {
                // On se connecte et on commence à écouter le flux d'événements du serveur.
                // Le `collect` est un opérateur terminal qui maintient la coroutine active
                // tant que la connexion WebSocket est ouverte.
                webSocketClient.connect(gameId, playerId).collect { event ->
                    handleServerEvent(event)
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Erreur de connexion", e)
                _uiState.value = GameUiState.Error("Connexion échouée")
            }
        }
    }

    /**
     * Le "routeur" d'événements entrants. Il est appelé à chaque fois qu'un message
     * arrive du serveur.
     */
    private fun handleServerEvent(event: ServerToClientEvent) {
        Log.d("GameViewModel", "Événement reçu du serveur: $event")
        if (event is ServerToClientEvent.GameStateUpdate) {
            // Le serveur a envoyé une mise à jour de l'état du jeu.

            // On reconstitue l'état complet du point de vue du joueur local
            // en fusionnant l'état public (`gameState`) et son chevalet privé (`playerRack`).
            val completeGameState = event.payload.gameState.copy(
                currentPlayerRack = event.payload.playerRack
            )            // On sauvegarde cet état comme la nouvelle "vérité officielle".
            officialGameState = completeGameState

            // On met à jour l'état de l'UI pour afficher le nouvel état de jeu.
            _uiState.value = GameUiState.Playing(
                gameData = completeGameState,
                localPlayerId = this.localPlayerId ?: "" // On passe l'ID pour que l'UI sache qui est "moi"
            )
            Log.d("GameViewModel", "État mis à jour avec le GameState du serveur.")
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

    // --- LOGIQUE DE JEU EN TEMPS RÉEL (CLIENT-SIDE) ---

    /**
     * Gère la pose d'une tuile depuis le chevalet sur le plateau.
     * C'est une action "optimiste" : l'UI est mise à jour immédiatement
     * avant même la confirmation du serveur.
     */
    fun onTilePlacedFromRack(rackIndex: Int, targetBoardPosition: BoardPosition) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        var newState = currentState.copy()
        val tileToMove = currentState.gameData.currentPlayerRack.getOrNull(rackIndex) ?: return
        if (!tileToMove.isJoker) {
            //1. On prépare les nouvelles données (nouveau chevalet, nouvelle liste de tuiles posées).
            val newRack = currentState.gameData.currentPlayerRack.toMutableList()
                .apply { removeAt(rackIndex) }
            val newPlacedTile = PlacedTile(tileToMove, targetBoardPosition)
            val updatedPlacedTiles = currentState.gameData.placedTiles + newPlacedTile
            val newBoard = officialGameState!!.board.withTiles(updatedPlacedTiles)
            // 2. On délègue le calcul complexe à une fonction privée.
            newState = calculateNewUiState(currentState, newBoard, updatedPlacedTiles, newRack)
        } else {
            // C'est un joker ! On met à jour l'état pour déclencher le dialogue.
            newState = currentState.copy(
                jokerSelectionState = GameUiState.JokerSelectionState.Selecting(
                    targetBoardPosition = targetBoardPosition,
                    tileId = tileToMove.id
                )
            )
            Log.d("GameViewModel", "Joker posé sur $targetBoardPosition. En attente de la sélection de la lettre...")
        }
        // 3. On applique le nouvel état calculé à l'UI.
        _uiState.value = newState
    }
    // ... (onTileMovedOnBoard, onTileReturnedToRack, etc. suivent une logique similaire)
    fun onTileMovedOnBoard(fromBoardPosition: BoardPosition, toBoardPosition: BoardPosition) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        val tileToMove = currentState.gameData.placedTiles.find { it.boardPosition == fromBoardPosition }?.tile ?: return

        val intermediatePlacedTiles = currentState.gameData.placedTiles.filter { it.boardPosition != fromBoardPosition }
        val finalPlacedTiles = intermediatePlacedTiles + PlacedTile(tileToMove, toBoardPosition)
        if (officialGameState == null) return
        val newBoard = officialGameState!!.board.withTiles(finalPlacedTiles)

        _uiState.value = calculateNewUiState(
            currentState = currentState,
            newBoard = newBoard,
            newPlacedTiles = finalPlacedTiles,
            newRack = currentState.gameData.currentPlayerRack
        )
    }

    fun onTileReturnedToRack(fromBoardPosition: BoardPosition) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Playing) return

        val tileToReturn = currentState.gameData.placedTiles.find { it.boardPosition == fromBoardPosition }?.tile ?: return
        val newPlacedTiles = currentState.gameData.placedTiles.filter { it.boardPosition != fromBoardPosition }
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


    /**
     * Le "cerveau" de la validation et du calcul de score côté client.
     *     * Cette fonction est appelée à chaque micro-interaction de l'utilisateur (pose, déplacement, retrait de tuile).
     * Elle prend l'état actuel et un "coup en cours" (newBoard, newPlacedTiles)
     * et retourne un nouvel état`Playing` complet avec le score mis à jour et
     * l'indicateur de validité (`isCurrentMoveValid`).
     *
     * @return Le nouvel état `GameUiState.Playing` à afficher.
     */
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
                ScoreCalculator.calculateScore(word, newBoard, newPlacedTiles.map { it.boardPosition })
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


    // --- ACTIONS FINALES DU JOUEUR ---

    /**
     * Appelée lorsque l'utilisateur appuie sur le bouton "JOUER".
     * Envoie le coup finalisé au serveur pour validation officielle.
     */
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

    /**
     * Annule le coup en cours et restaure l'état du jeu tel qu'il était
     * au début du tour.
     */
    fun onUndoMove() {
        // On vérifie que nous avons bien un état officiel à restaurer.
        if (officialGameState != null) {
            // La seule et unique action : on remplace l'état de l'UI
            // par la dernière version propre que nous avons sauvegardée.
            _uiState.value = GameUiState.Playing(
                gameData = officialGameState!!,
                localPlayerId = this.localPlayerId ?: ""
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

    /**
     * Appelée automatiquement lorsque le ViewModel est sur le point d'être détruit.
     * C'est l'endroit idéal pour nettoyer les ressources, comme fermer la connexion WebSocket.
     */
    override fun onCleared() {
        super.onCleared()
        webSocketClient.close()
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
    val jokerRackIndex = currentState.gameData.currentPlayerRack.indexOfFirst { it.id == selectionState.tileId }
    if (jokerRackIndex == -1) {
        Log.e("GameViewModel", "Impossible de retrouver le joker avec l'ID ${selectionState.tileId} sur le chevalet.")
        return
    }
    val jokerTile = currentState.gameData.currentPlayerRack[jokerRackIndex]

    // 2. On crée la nouvelle tuile joker avec la lettre assignée.
    val assignedJoker = jokerTile.copy(assignedLetter = letter.uppercase())

    // 3. On simule le placement de cette nouvelle tuile.
    //    a. On retire la tuile originale du chevalet.
    val newRack = currentState.gameData.currentPlayerRack.toMutableList().apply { removeAt(jokerRackIndex) }
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

    Log.d("GameViewModel", "Joker assigné à la lettre '$letter' et placé sur ${selectionState.targetBoardPosition}.")
}

/**
 * Appelé quand l'utilisateur annule la sélection du joker.
 * Remet le joker dans le chevalet.
 */
//fun onJokerSelectionDismissed() {
//    val currentState = _uiState.value
//    if (currentState !is GameUiState.Playing) return
//
//    val selectionState = currentState.jokerSelectionState
//    if (selectionState !is GameUiState.JokerSelectionState.Selecting) return
//
//    // On retire le joker du plateau et on le remet dans le chevalet
//    val updatedGameData = onTileReturnToRack(
//        position = selectionState.boardPosition,
//        returnToRack = true
//    )
//
//    // On ferme le dialog
//    _uiState.value = currentState.copy(
//        gameData = updatedGameData,
//        jokerSelectionState = null
//    )
//}

}