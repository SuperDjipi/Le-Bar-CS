package club.djipi.lebarcs.server

import club.djipi.lebarcs.shared.network.ClientToServerEvent
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val json = Json {
        ignoreUnknownKeys = true
        // Bonne pratique pour éviter les crashs si le serveur ajoute un champ
    }
    // --- Configuration des plugins Ktor ---
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        // On configure le même convertisseur JSON que sur le client
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    // --- Définition des routes ---
    routing {
        // C'est ici que l'on définit le "point de terminaison" pour notre WebSocket
        webSocket("/ws/{gameId}") { // Correspond à l'URL "ws://.../ws/some-game-id"
            val gameId = call.parameters["gameId"] ?: return@webSocket close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Game ID manquant"
                )
            )
            println("✅ Nouveau joueur connecté à la partie: $gameId")

            try {
                // Boucle pour écouter les messages entrants du client
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        // 1. On lit le contenu texte de la trame
                        val frameText = frame.readText()

                        // 2. On utilise notre propre instance de Json pour désérialiser la chaîne
                        val event = json.decodeFromString<ClientToServerEvent>(frameText)

                        // --- C'est ici que votre logique serveur commence ! ---
                        when (event) {
                            is ClientToServerEvent.JoinGame -> {
                                println("Événement reçu : JoinGame par le joueur ${event.playerId}")
                                // TODO:
                                // 1. Récupérer ou créer la partie 'gameId'
                                // 2. Ajouter le joueur
                                // 3. Envoyer le GameState complet à ce joueur
                                // val gameState = getGame(gameId).toGameStateForPlayer(event.playerId)
                                // sendSerialized(ServerToClientEvent.GameStateUpdate(gameState))
                            }

                            is ClientToServerEvent.PlayMove -> {
                                println("Événement reçu : PlayMove avec ${event.placedTiles.size} tuiles")
                                // TODO:
                                // 1. Récupérer la partie
                                // 2. Valider le coup (avec MoveValidator, Dictionary, etc.)
                                // 3. Si valide, mettre à jour l'état du jeu
                                // 4. Envoyer le nouveau GameState à TOUS les joueurs de la partie
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Erreur sur le WebSocket : ${e.localizedMessage}")
            } finally {
                println("👋 Joueur déconnecté de la partie: $gameId")
            }
        }
    }
}