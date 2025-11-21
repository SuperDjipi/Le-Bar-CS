package club.djipi.lebarcs.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import club.djipi.lebarcs.ui.screens.game.GameScreen
import club.djipi.lebarcs.ui.screens.game.GameViewModel
import club.djipi.lebarcs.ui.screens.home.HomeScreen
import club.djipi.lebarcs.ui.screens.lobby.LobbyScreen

/**
 * Définit toutes les routes (destinations) possibles de l'application
 * sous la forme d'une classe scellée (`sealed class`).
 *
 * L'utilisation d'une `sealed class` permet d'avoir une complétion automatique
 * et une sécurité de type lors de la définition des routes, prévenant les erreurs de frappe.
 */
sealed class Screen(val route: String) {
    /**
     * L'écran d'accueil principal de l'application.
     */
    object Home : Screen("home")

    /**
     * Représente le "flux de jeu" (Game Flow), un sous-graphe de navigation qui
     * englobe tout ce qui est lié à une partie spécifique (lobby, écran de jeu, etc.).
     *
     * Cette route accepte deux arguments :
     * - `gameId`: L'identifiant unique de la partie.
     * - `playerId`: L'identifiant unique du joueur local.
     *
     * Le but de ce groupe est de permettre le partage d'un `GameViewModel` entre
     * plusieurs écrans (LobbyScreen et GameScreen), assurant ainsi que l'état de la partie
     * et la connexion WebSocket persistent lors de la navigation entre ces écrans.
     */
    object GameFlow : Screen("game_flow/{gameId}/{playerId}") {
        /**
         * Construit la route de navigation complète avec les arguments requis.
         * @param gameId L'ID de la partie à rejoindre.
         * @param playerId L'ID du joueur local.
         * @return Une `String` formatée, par exemple "game_flow/123/xxxx-yyyy".
         */
        fun createRoute(gameId: String, playerId: String) = "game_flow/$gameId/$playerId"
    }

    /**
     * L'écran du lobby, où le joueur attend avant de commencer la partie.
     * Sa route est relative au `GameFlow`.
     * Note: Les arguments {gameId} sont ici redondants et pourraient être simplifiés
     * car ils sont déjà dans la route parente `GameFlow`.
     */
    object Lobby : Screen("lobby/{gameId}")

    /**
     * L'écran principal du jeu où se trouve le plateau.
     * Sa route est également relative au `GameFlow`.
     */
    object Game : Screen("game/{gameId}")
}

/**
 * Le composant principal qui définit la structure de navigation de toute l'application.
 *
 * Il utilise un `NavHost` pour héberger tous les écrans (Composables) et gère
 * les transitions entre eux en se basant sur les routes définies dans la `sealed class Screen`.
 *
 * @param modifier Le `Modifier` à appliquer au conteneur `NavHost`.
 * @param navController Le contrôleur qui gère la pile de navigation. Il est créé
 *                      par défaut avec `rememberNavController()` mais peut être fourni
 *                      de l'extérieur pour les tests ou des scénarios plus complexes.
 * @param startDestination La route de l'écran à afficher au lancement de l'application.
 */
@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    // NavHost est le conteneur qui affiche la destination actuelle du `navController`.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        /**
         * Définition de la destination pour l'écran d'accueil.
         */
        composable(Screen.Home.route) {
            HomeScreen(
                // La lambda `onNavigateToLobby` est appelée lorsque l'utilisateur
                // veut créer ou rejoindre une partie. Elle déclenche la navigation
                // vers le sous-graphe `GameFlow`.
                onNavigateToLobby = { gameId, playerId ->
                    navController.navigate(Screen.GameFlow.createRoute(gameId, playerId))
                }
            )
        }

        /**
         * Définition d'un graphe de navigation IMBRIQUÉ pour le "flux de jeu".
         *
         * L'utilisation de `navigation(...)` est la clé pour partager un ViewModel.
         * Tous les `composable` définis à l'intérieur de ce bloc partageront le même
         * scope de navigation (`game_flow`), et donc la même instance de `GameViewModel`.
         *
         * @param startDestination La route de départ à l'intérieur de ce sous-graphe (LobbyScreen).
         * @param route La route qui identifie ce groupe de navigation ("game_flow/{gameId}/{playerId}").
         */
        navigation(
            startDestination = Screen.Lobby.route,
            route = Screen.GameFlow.route,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.StringType })
        ) {
            /**
             * Définition de la destination pour l'écran du Lobby.
             */
            composable(Screen.Lobby.route) { backStackEntry ->
                // 'backStackEntry' contient les informations sur l'entrée actuelle dans la pile de navigation.

                // Pour obtenir le ViewModel partagé, on doit récupérer l'entrée de la pile
                // de navigation du *parent* (le groupe "game_flow").
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.GameFlow.route)
                }

                // On extrait les arguments `gameId` et `playerId` depuis la route du parent.
                val gameId = parentEntry.arguments?.getString("gameId") ?: ""
                val playerId = parentEntry.arguments?.getString("playerId") ?: ""

                // hiltViewModel(parentEntry) est la fonction magique qui demande à Hilt
                // de créer (ou de fournir) une instance de GameViewModel attachée au cycle
                // de vie du graphe de navigation "game_flow", et non à celui de l'écran Lobby seul.
                val gameViewModel: GameViewModel = hiltViewModel(parentEntry)

                // On informe le ViewModel de l'identité du joueur une seule fois,
                // grâce au `LaunchedEffect` qui s'exécute lorsque le `playerId` est disponible.
                LaunchedEffect(Unit) {
                    if (gameId.isNotBlank() && playerId.isNotBlank()) {
                        Log.d("NavGraph", "Lobby LaunchedEffect: $gameId, $playerId")

                        // 1. D'abord, on identifie le ViewModel.
                        gameViewModel.setLocalPlayerId(playerId)

                        // 2. ENSUITE, on lance la connexion.
                        gameViewModel.connectToGame(gameId)
                    }
                }

                LobbyScreen(
                    gameId = gameId,
                    viewModel = gameViewModel, // On passe le ViewModel partagé.
                    onNavigateToGame = {
                        // On navigue simplement vers la route "game", car on est déjà
                        // à l'intérieur du sous-graphe.
                        navController.navigate(Screen.Game.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            /**
             * Définition de la destination pour l'écran de Jeu.
             */
            composable(Screen.Game.route) { backStackEntry ->
                // On applique la MÊME logique pour récupérer le ViewModel partagé.
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.GameFlow.route)
                }
                // Hilt nous donnera LA MÊME instance de GameViewModel que celle créée
                // pour le LobbyScreen, car elles partagent le même `parentEntry`.
                val gameViewModel: GameViewModel = hiltViewModel(parentEntry)

                GameScreen(
                    viewModel = gameViewModel, // On passe le même ViewModel.
                    onNavigateBack = {
                        navController.popBackStack(Screen.Home.route, false)
                    }
                )
            }
        }
    }
}