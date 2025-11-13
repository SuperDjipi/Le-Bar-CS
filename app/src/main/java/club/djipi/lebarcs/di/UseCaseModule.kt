package club.djipi.lebarcs.di

import club.djipi.lebarcs.shared.domain.logic.Dictionary
import club.djipi.lebarcs.shared.domain.usecase.CalculateScoreUseCase
import club.djipi.lebarcs.shared.domain.usecase.ValidateMoveUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideValidateMoveUseCase(dictionary: Dictionary): ValidateMoveUseCase {
        // Hilt sait déjà comment fournir un 'Dictionary' grâce à votre AppModule
        return ValidateMoveUseCase(dictionary)
    }

    @Provides
    @ViewModelScoped
    fun provideCalculateScoreUseCase(): CalculateScoreUseCase {
        return CalculateScoreUseCase()
    }
}
