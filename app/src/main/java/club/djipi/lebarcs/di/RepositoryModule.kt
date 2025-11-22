package club.djipi.lebarcs.di

import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.data.repository.GameRepositoryImpl
import club.djipi.lebarcs.data.repository.HomeRepositoryImpl
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import club.djipi.lebarcs.shared.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

// Dans : app/src/main/java/club/djipi/lebarcs/di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGameRepository(
        // Le Repository a besoin de ces dépendances pour fonctionner
        webSocketClient: WebSocketClient,
        @ApplicationScope externalScope: CoroutineScope,
        httpClient: HttpClient
    ): GameRepository {
        return GameRepositoryImpl(webSocketClient, externalScope, httpClient)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        // Hilt sait comment fournir le HttpClient grâce au NetworkModule
        httpClient: HttpClient
    ): HomeRepository {
        return HomeRepositoryImpl(httpClient)
    }
}