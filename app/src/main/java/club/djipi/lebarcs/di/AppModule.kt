package club.djipi.lebarcs.di

import android.content.Context
import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.util.AndroidResourceReader
import club.djipi.lebarcs.util.ResourceReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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