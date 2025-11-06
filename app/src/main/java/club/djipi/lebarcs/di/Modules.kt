package club.djipi.lebarcs.di

import android.content.Context
import club.djipi.lebarcs.util.AndroidResourceReader
import androidx.room.Room
import club.djipi.lebarcs.data.local.ScrabbleDatabase
import club.djipi.lebarcs.data.local.dao.GameDao
import club.djipi.lebarcs.data.local.dao.PlayerDao
import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.data.repository.GameRepository
import club.djipi.lebarcs.data.repository.GameRepositoryImpl
import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.util.ResourceReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideScrabbleDatabase(
        @ApplicationContext context: Context
    ): ScrabbleDatabase {
        return Room.databaseBuilder(
            context,
            ScrabbleDatabase::class.java,
            ScrabbleDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideGameDao(database: ScrabbleDatabase): GameDao {
        return database.gameDao()
    }
    
    @Provides
    @Singleton
    fun providePlayerDao(database: ScrabbleDatabase): PlayerDao {
        return database.playerDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }
    
    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(Android) {
            install(WebSockets)
            
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideWebSocketClient(
        httpClient: HttpClient,
        json: Json
    ): WebSocketClient {
        return WebSocketClient(httpClient, json)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideGameRepository(
        gameDao: GameDao,
        playerDao: PlayerDao,
        webSocketClient: WebSocketClient
    ): GameRepository {
        return GameRepositoryImpl(gameDao, playerDao, webSocketClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideServerUrl(): String {
        // À configurer selon votre environnement
        return "ws://10.0.2.2:8080" // Émulateur Android
        // return "ws://localhost:8080" // Appareil physique en USB
        // return "ws://YOUR_SERVER_IP:8080" // Production
    }

    @Provides
    @Singleton
    fun provideResourceReader(@ApplicationContext context: Context): ResourceReader {
        // Hilt nous fournit le contexte, et nous créons la bonne classe.
        return AndroidResourceReader(context)
    }
    // On dit à Hilt comment créer un Dictionary
    @Provides
    @Singleton
    fun provideDictionary(resourceReader: ResourceReader): Dictionary {
        return Dictionary(resourceReader)
    }

}

