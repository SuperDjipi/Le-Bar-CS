package club.djipi.lebarcs.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.domain.model.*
import club.djipi.lebarcs.shared.domain.model.*
import club.djipi.lebarcs.ui.screens.game.components.*
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

/**
 * Écran de test pour visualiser tous les composants UI
 * Utile pour le développement et le debug
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestUIScreen() {
    var selectedTileIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test des composants UI") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section Tuiles
            Text("Tuiles individuelles", style = MaterialTheme.typography.titleLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TileView(tile = Tile(letter = 'A', points = 1))
                TileView(tile = Tile(letter = 'K', points = 10))
                TileView(tile = Tile(letter = 'Z', points = 10))
                TileView(tile = Tile(letter = '_', points = 0, isJoker = true))
                TileView(tile = Tile(letter = 'E', points = 1), isSelected = true)
            }

            Divider()

            // Section Chevalet
            Text("Chevalet complet", style = MaterialTheme.typography.titleLarge)
            TileRack(
                tiles = listOf(
                    Tile(letter = 'S', points = 1),
                    Tile(letter = 'C', points = 3),
                    Tile(letter = 'R', points = 1),
                    Tile(letter = 'A', points = 1),
                    Tile(letter = 'B', points = 3),
                    Tile(letter = 'L', points = 1),
                    Tile(letter = 'E', points = 1)
                ),
                selectedIndex = selectedTileIndex,
                onTileClick = {
                    selectedTileIndex = if (selectedTileIndex == it) null else it
                }
            )

            Text("Chevalet partiel", style = MaterialTheme.typography.titleLarge)
            TileRack(
                tiles = listOf(
                    Tile(letter = 'H', points = 4),
                    Tile(letter = 'E', points = 1),
                    Tile(letter = 'L', points = 1)
                )
            )

            Divider()

            // Section Scores
            Text("Tableau des scores", style = MaterialTheme.typography.titleLarge)
            ScoreBoard(
                players = listOf(
                    Player("1", "Alice", 125, emptyList()),
                    Player("2", "Bob", 98, emptyList()),
                    Player("3", "Charlie", 87, emptyList())
                ),
                currentPlayerIndex = 0
            )

            Divider()

            // Section Cases
            Text("Cases du plateau", style = MaterialTheme.typography.titleLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BoardCellView(
                    cell = BoardCell(Position(0, 0), BonusType.NONE)
                )
                BoardCellView(
                    cell = BoardCell(Position(0, 1), BonusType.DOUBLE_LETTER)
                )
                BoardCellView(
                    cell = BoardCell(Position(0, 2), BonusType.TRIPLE_LETTER)
                )
                BoardCellView(
                    cell = BoardCell(Position(0, 3), BonusType.DOUBLE_WORD)
                )
                BoardCellView(
                    cell = BoardCell(Position(0, 4), BonusType.TRIPLE_WORD)
                )
                BoardCellView(
                    cell = BoardCell(Position(7, 7), BonusType.CENTER)
                )
            }

            Text("Cases avec tuiles", style = MaterialTheme.typography.titleLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BoardCellView(
                    cell = BoardCell(
                        Position(0, 0),
                        BonusType.NONE,
                        Tile(letter = 'H', points = 4),
                        isLocked = true
                    )
                )
                BoardCellView(
                    cell = BoardCell(
                        Position(0, 1),
                        BonusType.DOUBLE_LETTER,
                        Tile(letter = 'E', points = 1),
                        isLocked = true
                    )
                )
                BoardCellView(
                    cell = BoardCell(
                        Position(0, 2),
                        BonusType.NONE,
                        Tile(letter = 'L', points = 1),
                        isLocked = true
                    )
                )
            }

            Divider()

            // Section Plateau miniature
            Text("Plateau complet (miniature)", style = MaterialTheme.typography.titleLarge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                BoardView(
                    board = Board(),
                    cellSize = 20,
                    onCellClick = { position ->
                        println("Cliqué sur: ligne=${position.row + 1}, col=${'A' + position.col}")
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
fun TestUIScreenPreview() {
    LeBarCSTheme {
        TestUIScreen()
    }
}