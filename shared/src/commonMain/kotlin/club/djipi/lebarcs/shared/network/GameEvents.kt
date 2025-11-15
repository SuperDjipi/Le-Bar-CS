package club.djipi.lebarcs.shared.network

import club.djipi.lebarcs.shared.domain.model.GameState
import club.djipi.lebarcs.shared.domain.model.PlacedTile
import club.djipi.lebarcs.shared.domain.model.Tile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contient tous les événements que le CLIENT peut envoyer au SERVEUR.
 */
@Serializable
data class PlayMovePayload(
    val placedTiles: List<PlacedTile>
)
@Serializable
sealed class ClientToServerEvent {
    /** Le client demande à rejoindre une partie spécifique. */
    @Serializable
    @SerialName("JOIN_GAME")
    data class JoinGame(val gameId: String, val playerId: String) : ClientToServerEvent()

    /** Le client propose de jouer un coup. */
    @Serializable
    @SerialName("PLAY_MOVE")
    data class PlayMove(val payload: PlayMovePayload) : ClientToServerEvent()
}

/**
 * Contient tous les événements que le SERVEUR peut envoyer aux CLIENTS.
 */
@Serializable
sealed class ServerToClientEvent {
    /**
     * Mise à jour complète de l'état du jeu. C'est le message principal.
     * Le serveur envoie cet objet pour synchroniser tous les joueurs.
     */
    @Serializable
    @SerialName("GAME_STATE_UPDATE")
    data class GameStateUpdate(
        // On encapsule dans un 'payload' pour correspondre au serveur
        val payload: GameStateUpdatePayload
    ) : ServerToClientEvent()

    /**
     * Notification d'une erreur spécifique au joueur.
     * Ex: "Votre coup est invalide."
     */
    @Serializable
    @SerialName("ERROR")
    data class ErrorMessage(
        val payload: ErrorPayload
    ) : ServerToClientEvent()
}

// On crée des classes pour les 'payloads'
@Serializable
data class GameStateUpdatePayload(val gameState: GameState, val playerRack: List<Tile>)

@Serializable
data class ErrorPayload(val message: String)