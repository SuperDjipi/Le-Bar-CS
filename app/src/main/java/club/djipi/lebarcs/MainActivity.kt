package club.djipi.lebarcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import club.djipi.lebarcs.ui.navigation.NavGraph
import club.djipi.lebarcs.ui.theme.LeBarCSTheme
import dagger.hilt.android.AndroidEntryPoint


/**
 * L'Activité principale et le point d'entrée unique de l'application LeBarCS.
 *
 * Cette activité a pour seul rôle de mettre en place l'environnement de base
 * pour une application Jetpack Compose. Elle ne contient aucune logique métier
 * ou d'interface utilisateur complexe.
 *
 * @see ComponentActivity C'est la classe de base pour une activité simple qui utilise Jetpack Compose.
 * @see AndroidEntryPoint Cette annotation est cruciale pour l'injection de dépendances avec Hilt.
 *                        Elle indique à Hilt que cette activité (et les fragments ou vues
 *                        qui en dépendent) peut recevoir des dépendances injectées.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Cette méthode est appelée lorsque l'Activité est créée pour la première fois.
     * C'est ici que toute l'initialisation de l'interface utilisateur a lieu.
     *
     * @param savedInstanceState Si l'activité est recréée après avoir été détruite
     *                           (par exemple, lors d'une rotation de l'écran), ce Bundle
     *                           contient l'état précédemment sauvegardé. Nous ne l'utilisons
     *                           pas directement car Jetpack Compose et les ViewModels
     *                           gèrent l'état de manière plus moderne.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent est la fonction qui relie le monde des "Activités" Android
        // au monde déclaratif de "Jetpack Compose". Tout ce qui se trouve
        // à l'intérieur de cette lambda sera rendu par Compose.
        setContent {
            // LeBarCSTheme est notre Composable de thème personnalisé. Il enveloppe
            // toute l'application pour fournir des couleurs, des typographies et des
            // formes cohérentes, en s'appuyant sur Material Design 3.
            LeBarCSTheme {

                // Surface est un conteneur de base de Material Design. Il fournit
                // une couleur de fond par défaut (issue du thème) et gère l'élévation.
                // Le modifier fillMaxSize garantit que notre application prend tout
                // l'espace disponible à l'écran.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Création du NavController :
                    //    rememberNavController() crée et mémorise un contrôleur de navigation.
                    //    Il est "remembered" (mémorisé) pour ne pas être recréé à chaque
                    //    recomposition, ce qui lui permet de conserver l'état de la navigation
                    //    (la pile d'écrans, l'écran actuel, etc.).
                    val navController = rememberNavController()

                    // 2. Initialisation du Graphe de Navigation :
                    //    On appelle notre Composable NavGraph, qui est le "cerveau" de la
                    //    navigation de l'application. On lui passe le navController
                    //    pour qu'il puisse exécuter les actions de navigation (naviguer vers
                    //    un nouvel écran, revenir en arrière, etc.).
                    //    C'est ce NavGraph qui définit toutes les routes possibles
                    //    ("home", "game_flow/{gameId}/{playerId}", etc.) et quel Composable
                    //    afficher pour chaque route.
                    NavGraph(navController = navController)
                }
            }
        }
    }
}