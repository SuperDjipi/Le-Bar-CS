package club.djipi.lebarcs.di

import android.content.Context
import androidx.room.Room
import club.djipi.lebarcs.data.local.ScrabbleDatabase
import club.djipi.lebarcs.data.local.dao.GameDao
import club.djipi.lebarcs.data.local.dao.PlayerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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