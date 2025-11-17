package club.djipi.lebarcs.data.local

import android.util.Log
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

// On définit les clés
private val LOCAL_PLAYER_ID_KEY = stringPreferencesKey("local_player_id")
private val LOCAL_PLAYER_NAME_KEY = stringPreferencesKey("local_player_name")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val localPlayerIdKey = stringPreferencesKey("local_player_id")
    // On crée un Flow qui observe les changements du nom.
    val playerName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_PLAYER_NAME_KEY]
    }

    suspend fun savePlayerName(name: String) {
        context.dataStore.edit { settings ->
            settings[LOCAL_PLAYER_NAME_KEY] = name
        }
    }

    suspend fun getLocalPlayerId(): String {
        val preferences = context.dataStore.data.first()
        var playerId = preferences[localPlayerIdKey]

        if (playerId == null) {
            // Si l'ID n'existe pas, on en crée un et on le sauvegarde
            playerId = UUID.randomUUID().toString()
            context.dataStore.edit { settings ->
                settings[localPlayerIdKey] = playerId
            }
            Log.d("UserPrefs", "Nouvel ID joueur local créé et sauvegardé : $playerId")
        } else {
            Log.d("UserPrefs", "ID joueur local existant chargé : $playerId")
        }
        return playerId
    }
}
