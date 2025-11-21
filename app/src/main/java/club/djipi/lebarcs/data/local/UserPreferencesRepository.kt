package club.djipi.lebarcs.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Le DataStore est défini une seule fois au niveau du fichier. C'est parfait.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // On définit les clés de manière privée et statique. C'est une bonne pratique.
    private object PreferencesKeys {
        val PLAYER_ID = stringPreferencesKey("local_player_id")
        val PLAYER_NAME = stringPreferencesKey("local_player_name")
    }

    suspend fun createAndSaveNewProfile(name: String, playerId: String) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.PLAYER_ID] = playerId
            settings[PreferencesKeys.PLAYER_NAME] = name
        }
        Log.d("UserPrefs", "Profil sauvegardé localement -> Nom: $name, ID: $playerId")
    }

    /**
     * Récupère l'ID du joueur local sauvegardé, SANS en créer un nouveau.
     * @return L'ID du joueur s'il existe, sinon null.
     */
    suspend fun getLocalPlayerId(): String? {
        // .firstOrNull() est plus sûr, il retourne null si le Flow est vide,
        // ce qui ne devrait pas arriver, mais c'est une bonne pratique.
        val preferences = context.dataStore.data.firstOrNull()
        val playerId = preferences?.get(PreferencesKeys.PLAYER_ID)
        if (playerId != null) {
            Log.d("UserPrefs", "ID joueur local existant chargé : $playerId")
        } else {
            Log.d("UserPrefs", "Aucun ID joueur local trouvé.")
        }
        return playerId
    }

    /**
     * Récupère le nom du joueur local sauvegardé.
     * @return Le nom du joueur s'il existe, sinon null.
     */
    suspend fun getPlayerName(): String? {
        val preferences = context.dataStore.data.firstOrNull()
        val playerName = preferences?.get(PreferencesKeys.PLAYER_NAME)
        if (playerName != null) {
            Log.d("UserPrefs", "Nom joueur local existant chargé : $playerName")
        } else {
            Log.d("UserPrefs", "Aucun nom de joueur local trouvé.")
        }
        return playerName
    }
}

