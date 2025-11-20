package club.djipi.lebarcs.shared.domain.logic

import club.djipi.lebarcs.shared.domain.model.Board
import club.djipi.lebarcs.shared.domain.model.BoardPosition

object MoveValidator {

    /**
     * Valide la géométrie d'un coup.
     * Vérifie que toutes les tuiles posées sont sur une seule ligne (horizontale ou verticale).
     * @param placedTiles Les positions des tuiles qui viennent d'être jouées.
     * @return true si le placement est valide, false sinon.
     */
    fun isPlacementValid(board: Board, placedTiles: Set<BoardPosition>): Boolean {
        if (placedTiles.size <= 1) {
            // Un coup d'une seule tuile est toujours aligné.
            // On vérifiera la connexion à une tuile existante ailleurs.
            return true
        }

        // 1. Vérifier que les NOUVELLES tuiles sont bien alignées
        val allOnSameRow = placedTiles.map { it.row }.toSet().size == 1
        val allOnSameCol = placedTiles.map { it.col }.toSet().size == 1

        if (!allOnSameRow && !allOnSameCol) {
            println("Validation échec : les tuiles posées ne sont pas sur la même ligne/colonne.")
            return false
        }

        // 2. Vérifier la contiguïté du mot principal formé
        if (allOnSameRow) {
            // Coup horizontal
            val row = placedTiles.first().row
            val minCol = placedTiles.minOf { it.col }
            val maxCol = placedTiles.maxOf { it.col }

            // On vérifie toutes les cases entre la première et la dernière NOUVELLE tuile
            for (col in minCol..maxCol) {
                // S'il y a un trou (case vide), c'est invalide
                if (board.getTile(BoardPosition(row, col)) == null && BoardPosition(row, col) !in placedTiles) {
                    println("Validation échec : trou dans le mot principal horizontal.")
                    return false
                }
            }
        } else { // Coup vertical
            val col = placedTiles.first().col
            val minRow = placedTiles.minOf { it.row }
            val maxRow = placedTiles.maxOf { it.row }

            for (row in minRow..maxRow) {
                if (board.getTile(BoardPosition(row, col)) == null && BoardPosition(row, col) !in placedTiles) {
                    println("Validation échec : trou dans le mot principal vertical.")
                    return false
                }
            }
        }

            println("Validation placement : OK !")
            return true
        }


fun isMoveConnected(board: Board, placedTiles: Set<BoardPosition>, turnNumber: Int): Boolean {
    // Règle du premier tour : le coup doit passer par le centre.
    if (turnNumber == 1) {
        val centerBoardPosition = BoardPosition(Board.SIZE / 2, Board.SIZE / 2) // (7,7)
        return centerBoardPosition in placedTiles
    }

    // Règle des tours suivants : au moins une tuile doit être adjacente à une tuile existante.
    val adjacentOffsets = listOf(BoardPosition(-1, 0), BoardPosition(1, 0), BoardPosition(0, -1), BoardPosition(0, 1))

    for (pos in placedTiles) {
        for (offset in adjacentOffsets) {
            val adjacentPos = BoardPosition(pos.row + offset.row, pos.col + offset.col)
            if (adjacentPos.row in 0 until Board.SIZE && adjacentPos.col in 0 until Board.SIZE) {
                // Si la case adjacente a une tuile ET qu'elle ne fait pas partie du coup actuel
                if (board.getTile(adjacentPos) != null && adjacentPos !in placedTiles) {
                    return true // C'est connecté !
                }
            }
        }
    }

    // Si on a vérifié toutes les tuiles et leurs voisins sans trouver de connexion...
    return false
}}