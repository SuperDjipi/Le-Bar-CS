package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.Position
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

/**
 * Composant du plateau de Scrabble 15x15
 */
private const val TAG = "BoardView"

@Composable
fun BoardView(
    board: Board,
    modifier: Modifier = Modifier,
    cellSize: Int = 35,
    dragDropManager: DragDropManager? = null,
    onCellClick: (Position) -> Unit = {}
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2F4F4F))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
        ) {
            // En-tête avec les numéros de colonnes (A-O)
            Row {
                // Coin vide
                Spacer(modifier = Modifier.size(20.dp))

                // Lettres des colonnes
                for (col in 0 until Board.SIZE) {
                    Box(
                        modifier = Modifier.size(cellSize.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + col).toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Plateau avec numéros de lignes
            board.cells.forEachIndexed { rowIndex, row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Numéro de ligne
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (rowIndex + 1).toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Cases de la ligne
                    row.forEach { cell ->
                        if (dragDropManager != null) {
                            // Version avec drag & drop
                            DroppableBoardCell(
                                cell = cell,
                                dragDropManager = dragDropManager,
                                size = cellSize.dp,
                                onClick = { onCellClick(cell.position) }
                            )
                            Log.d(TAG, "BoardView: $cell")
                        } else {
                            // Version simple
                            BoardCellView(
                                cell = cell,
                                size = cellSize.dp,
                                onClick = { onCellClick(cell.position) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 600, heightDp = 600)
@Composable
fun BoardViewPreview() {
    LeBarCSTheme {
        BoardView(
            board = Board(),
            cellSize = 35
        )
    }
}