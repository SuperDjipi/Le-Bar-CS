package club.djipi.lebarcs.shared.domain.logic

import club.djipi.lebarcs.shared.domain.model.Position

object MoveValidator {

    /**
     * Valide la géométrie d'un coup.
     * Vérifie que toutes les tuiles posées sont sur une seule ligne (horizontale ou verticale).
     * @param placedTiles Les positions des tuiles qui viennent d'être jouées.
     * @return true si le placement est valide, false sinon.
     */
    fun isPlacementValid(placedTiles: Set<Position>): Boolean {
        if (placedTiles.size <= 1) {
            // Un coup d'une seule tuile est toujours aligné.
            // On vérifiera la connexion à une tuile existante ailleurs.
            return true
        }

        // On vérifie si toutes les tuiles sont sur la même ligne (row)
        val allOnSameRow = placedTiles.map { it.row }.toSet().size == 1

        // On vérifie si toutes les tuiles sont sur la même colonne (col)
        val allOnSameCol = placedTiles.map { it.col }.toSet().size ==1

        if (!allOnSameRow && !allOnSameCol) {
            // Les tuiles ne sont ni sur la même ligne, ni sur la même colonne.
            println("Validation échec : les tuiles ne sont pas alignées.")
            return false}

        // Maintenant, on vérifie la contiguïté (pas de "trous" dans le mot)
        if (allOnSameRow) {
            // Coup horizontal
            val cols = placedTiles.map { it.col }.sorted()
            for (i in 0 until cols.size - 1) {
                if (cols[i+1] != cols[i] + 1) {
                    println("Validation échec : trou dans le mot horizontal.")
                    return false // Il y a un trou
                                }
                }
            } else { // Coup vertical
                val rows = placedTiles.map { it.row }.sorted()
                for (i in 0 until rows.size - 1) {
                    if (rows[i+1] != rows[i] + 1) {
                        println("Validation échec : trou dans le mot vertical.")
                        return false // Il y a un trou
                    }
                }
            }

            println("Validation placement : OK !")
            return true
        }
    }