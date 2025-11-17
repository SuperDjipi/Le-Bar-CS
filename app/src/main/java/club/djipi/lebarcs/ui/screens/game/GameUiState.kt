package club.djipi.lebarcs.ui.screens.game

import club.djipi.lebarcs.shared.domain.logic.FoundWord
import club.djipi.lebarcs.shared.domain.model.GameState

/**
 * Définit tous les états possibles de l'interface utilisateur pour l'écran de jeu.
 *
 * En tant que `sealed class` (classe scellée), `GameUiState` garantit que toutes les variations
 * possibles de l'état de l'UI sont connues à l'avance (Loading, Playing, Error).
 * Cela permet d'utiliser des expressions `when` exhaustives dans les `Composables`
 * pour s'assurer que tous les cas sont gérés.
 *
 * C'est le "Modèle" dans une architecture MVI (Model-View-Intent) pour cet écran.
 */
sealed class GameUiState {

    /**
     * Représente l'état de chargement initial, par exemple lorsque l'application
     * est en train d'établir la connexion avec le serveur.
     * L'UI affichera typiquement une roue de progression (`CircularProgressIndicator`).
     */
    object Loading : GameUiState()

    /**
     * Représente l'état principal où le jeu est en cours.
     *
     * Cette `data class` contient toutes les informations nécessaires pour que l'UI
     * * puisse s'afficher et être interactive. Elle est immuable, ce qui garantit
     * un flux de données prévisible.
     *
     * @property gameData L'état officiel et complet du jeu (`GameState`),
     *                      généralement reçu du serveur. C'est la "source de vérité"
     *                      pour le plateau, les scores, le joueur actuel, etc.
     * @property localPlayerId L'identifiant unique du joueur qui utilise l'appareil.
     *                         Essentiel pour que l'UI sache qui est "moi" et puisse
     *                         adapter son comportement (par exemple, activer le bouton "JOUER").
     * @property selectedTileIndex L'index de la tuile actuellement sélectionnée sur le chevalet.
     *                             `null` si aucune n'est sélectionnée.
     * @property jokerSelectionTarget Si un joker est posé, cette propriété contient la
     *                              `Position` sur le plateau où le dialogue de sélection
     *                              de lettre doit apparaître. `null` sinon.
     * @property foundWordsForCurrentMove La liste des mots formés par le coup en cours,
     *                                  calculée en temps réel par le `GameViewModel`.
     * @property currentMoveScore Le score du coup en cours, calculé en temps réel.
     * @property isCurrentMoveValid `true` si le coup en cours est valide (selon toutes les
     *                          règles du jeu : connexion, dictionnaire, etc.), `false` sinon.
     */
    data class Playing(
        val gameData: GameState,
        val localPlayerId: String,
        val selectedTileIndex: Int? = null,
        val foundWordsForCurrentMove: List<FoundWord> = emptyList(),
        val currentMoveScore: Int = 0,
        val isCurrentMoveValid: Boolean = false
    ) : GameUiState() {

        /**
         * Une propriété calculée "de convenance" qui indique si c'est le tour du joueur local.
         *
         * Elle encapsule la logique de comparaison des ID, rendant le code de l'UI
         * qui l'utilise (`Button(enabled = isLocalPlayerTurn, ...)`) plus lisible.
         */
        val isLocalPlayerTurn: Boolean
            get() = gameData.players.getOrNull(gameData.currentPlayerIndex)?.id == localPlayerId
    }

    /*** Représente un état d'erreur irrécupérable (par exemple, échec de la connexion).
     *
     * @param message Le message d'erreur à afficher à l'utilisateur.
     */
    data class Error(val message: String) : GameUiState()
}