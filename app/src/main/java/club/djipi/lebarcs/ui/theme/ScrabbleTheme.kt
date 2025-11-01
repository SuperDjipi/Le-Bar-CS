package club.djipi.lebarcs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Couleurs du Scrabble classique
val ScrabbleBeige = Color(0xFFE8DCC4)
val ScrabbleDarkBeige = Color(0xFFD4C5A9)
val ScrabbleBrown = Color(0xFF8B7355)
val ScrabbleRed = Color(0xFFDC143C)
val ScrabbleBlue = Color(0xFF4169E1)

// Couleurs pour les bonus
val DoubleLetterColor = Color(0xFF87CEEB) // Bleu clair
val TripleLetterColor = Color(0xFF0000CD) // Bleu foncé
val DoubleWordColor = Color(0xFFFFB6C1) // Rose clair
val TripleWordColor = Color(0xFFFF0000) // Rouge
val CenterStarColor = Color(0xFFFFD700) // Or

private val DarkColorScheme = darkColorScheme(
    primary = ScrabbleBrown,
    secondary = ScrabbleBeige,
    tertiary = ScrabbleDarkBeige,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFFFFBFE),
    onSurface = Color(0xFFFFFBFE)
)

private val LightColorScheme = lightColorScheme(
    primary = ScrabbleBrown,
    secondary = ScrabbleBeige,
    tertiary = ScrabbleDarkBeige,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun ScrabbleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
