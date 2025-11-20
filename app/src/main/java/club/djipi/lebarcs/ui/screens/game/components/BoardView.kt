package club.djipi.lebarcs.ui.screens.game.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragDropManager
import club.djipi.lebarcs.ui.screens.game.dragdrop.DragSource
import club.djipi.lebarcs.ui.theme.LeBarCSTheme

private const val TAG = "BoardView"

@Composable
fun BoardView(
    board: Board,
    modifier: Modifier = Modifier,
    cellSize: Int = 35,
    dragDropManager: DragDropManager? = null
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            // On peut réduire le padding maintenant qu'on n'a plus les coordonnées
            .padding(4.dp)
            // On peut centrer le plateau dans le Box parent
            .wrapContentSize(Alignment.Center)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
        ) {
            // On parcourt directement les lignes du plateau
            board.cells.forEach { row ->
                Row {
                    // Cases de la ligne
                    row.forEach { cell ->
                        DroppableBoardCell(
                            cell = cell,
                            dragDropManager = dragDropManager,
                            size = cellSize.dp
                        )
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
        val previewDragDropManager = remember { DragDropManager() }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2F4F4F))) {
            BoardView(
                board = Board(),
                cellSize = 35,
                dragDropManager = previewDragDropManager
            )
        }
    }
}