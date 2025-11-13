package club.djipi.lebarcs.data.remote

import androidx.activity.result.launch
import club.djipi.lebarcs.shared.network.ClientToServerEvent
import club.djipi.lebarcs.shared.network.ServerToClientEvent
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(

    // On s'assure d'avoir une instance de Json disponible dans la classe
    private val httpClient: HttpClient,
    private val json: Json,
private val serverUrl: String
) {
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(gameId: String): Flow<ServerToClientEvent> {
        try {
            session = httpClient.webSocketSession {
                url("ws://10.0.2.2:8080/ws/$gameId")
            }
            return session!!.incoming
                .consumeAsFlow()
                // On utilise mapNotNull pour filtrer automatiquement les résultats nuls
                .map { frame ->
                    // On ne traite que les trames de type Texte
                    if (frame is Frame.Text) {
                        println("WSClient : ${ frame.readText() }", )
                        try {
                            // On désérialise la chaîne de caractères en objet ServerToClientEvent
                            json.decodeFromString<ServerToClientEvent>(frame.readText())
                        } catch (e: Exception) {// Si la désérialisation échoue, on logue l'erreur et on retourne null
                            e.printStackTrace()
                            null
                        }
                    } else {
                        null
                        // Si ce n'est pas une trame de texte, on l'ignore (retourne null)                        null
                    }
                }
                .filterNotNull()

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun sendEvent(event: ClientToServerEvent) {
        try {
            val jsonString = json.encodeToString(event)
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- DÉBUT DE L'AJOUT DE LA FONCTION MANQUANTE ---
    /**
     * Ferme la session WebSocket et le client Ktor pour libérer les ressources.
     * C'est une fonction normale (non suspend) pour pouvoir être appelée depuis onCleared() du ViewModel.
     */
    fun close() {
        // La fermeture de la session peut prendre du temps, on la lance dans une coroutine
        // pour ne pas bloquer le thread principal, mais on n'a pas besoin d'attendre la fin.
        // Un scope simple est suffisant ici.
        CoroutineScope(Dispatchers.IO).launch {
            session?.close()
        }
        httpClient.close()
        println("WebSocketClient fermé.")
    }
}
