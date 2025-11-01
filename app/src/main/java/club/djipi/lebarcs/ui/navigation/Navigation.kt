package club.djipi.lebarcs.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import club.djipi.lebarcs.ui.screens.home.HomeScreen
import club.djipi.lebarcs.ui.screens.lobby.LobbyScreen
import club.djipi.lebarcs.ui.screens.game.GameScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Lobby : Screen("lobby/{gameId}") {
        fun createRoute(gameId: String) = "lobby/$gameId"
    }
    object Game : Screen("game/{gameId}") {
        fun createRoute(gameId: String) = "game/$gameId"
    }
}

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
                    navController.navigate(Screen.Lobby.createRoute(gameId))
                }
            )
        }
        
        composable(Screen.Lobby.route) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            LobbyScreen(
                gameId = gameId,
                onNavigateToGame = {
                    navController.navigate(Screen.Game.createRoute(gameId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Game.route) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            GameScreen(
                gameId = gameId,
                onNavigateBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
