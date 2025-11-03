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
import club.djipi.lebarcs.ui.navigation.NavGraph
import club.djipi.lebarcs.ui.theme.LeBarCSTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//
//        setContent {
//            ScrabbleTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    NavGraph(modifier = Modifier.padding(innerPadding))
//                }
//            }
        setContent {
            LeBarCSTheme {
                // HelloScrabbleApp()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
@Composable
fun HelloScrabbleApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hello, LeBarCS App !",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}