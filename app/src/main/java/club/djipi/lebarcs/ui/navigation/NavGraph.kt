package club.djipi.lebarcs.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation // <- L'import clé pour le graphe imbriqué
import androidx.navigation.compose.rememberNavController
import club.djipi.lebarcs.ui.screens.home.HomeScreen
import club.djipi.lebarcs.ui.screens.lobby.LobbyScreen
import club.djipi.lebarcs.ui.screens.game.GameScreen
import club.djipi.lebarcs.ui.screens.game.GameViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    // La route du groupe de jeu. On lui passe le gameId
    object GameFlow : Screen("game_flow/{gameId}") {
        fun createRoute(gameId: String) = "game_flow/$gameId"
    }
    object Lobby : Screen("lobby/{gameId}") {
        fun createRoute(gameId: String) = "lobby/$gameId"
    }
    object Game : Screen("game/{gameId}") {
        fun createRoute(gameId: String) = "game/$gameId"
    }
}

// --- ON DÉFINIT UNE NOUVELLE ROUTE POUR LE GROUPE ---
const val GAME_FLOW_ROUTE = "game_flow"

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLobby = { gameId ->
                    navController.navigate(Screen.GameFlow.createRoute(gameId))
                }
            )
        }

        navigation(
            // La destination de départ de ce sous-graphe est la route du Lobby
            startDestination = Screen.Lobby.route,
            // On donne un nom à ce "groupe" de routes
            route = Screen.GameFlow.route
        ) {
            // Définition de l'écran Lobby à l'intérieur du groupe
            composable(Screen.Lobby.route) { backStackEntry ->
                // --- ON RÉCUPÈRE LE gameId DEPUIS LE PARENT ---
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.GameFlow.route)
                }
                val gameId = parentEntry.arguments?.getString("gameId") ?: ""
                // 2. On passe cette entrée à Hilt pour qu'il attache le ViewModel au bon scope.
                val gameViewModel: GameViewModel = hiltViewModel(parentEntry)


                LobbyScreen(
                    gameId = gameId,
                    // On passe le ViewModel partagé à l'écran
                    viewModel = gameViewModel,
                    onNavigateToGame = {
                        navController.navigate(Screen.Game.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Définition de l'écran Game à l'intérieur du même groupe
            composable(Screen.Game.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.GameFlow.route)
                }
                val gameViewModel: GameViewModel = hiltViewModel(parentEntry)

                GameScreen(
                    // Le gameId n'est plus nécessaire car le ViewModel est déjà initialisé
                    viewModel = gameViewModel,
                    onNavigateBack = {
                        //navController.popBackStack(Screen.Home.route, inclusive = false)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}