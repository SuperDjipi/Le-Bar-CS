package club.djipi.lebarcs.di

import club.djipi.lebarcs.data.remote.WebSocketClient
import club.djipi.lebarcs.data.repository.GameRepositoryImpl
import club.djipi.lebarcs.shared.domain.repository.GameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGameRepository(
        // Le Repository n'a besoin QUE de ces deux dépendances
        webSocketClient: WebSocketClient,
        @ApplicationScope externalScope: CoroutineScope
    ): GameRepository {
        return GameRepositoryImpl(webSocketClient, externalScope)
    }
}