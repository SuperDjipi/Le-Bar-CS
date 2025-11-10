package club.djipi.lebarcs.shared.domain.model

import club.djipi.lebarcs.shared.generateUUID
import kotlinx.serialization.Serializable

@Serializable
data class PlacedTile(
    val tile: Tile,
    val position: Position
)

/**
 * Tuile du jeu
 */
@Serializable
data class Tile(
    val id: String = generateUUID(),
    val letter: Char,       // 'A'-'Z' ou '_' pour joker
    val points: Int,        // Valeur en points
    val isJoker: Boolean = false
) {
    companion object {
        // Valeurs standard du Scrabble français
        val value = mapOf(
            'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1,
            'F' to 4, 'G' to 2, 'H' to 4, 'I' to 1, 'J' to 8,
            'K' to 10, 'L' to 1, 'M' to 2, 'N' to 1, 'O' to 1,
            'P' to 3, 'Q' to 8, 'R' to 1, 'S' to 1, 'T' to 1,
            'U' to 1, 'V' to 4, 'W' to 10, 'X' to 10, 'Y' to 10,
            'Z' to 10, '_' to 0
        )

        // Distribution des tuiles en français
        val distribution = mapOf(
            'A' to 9, 'B' to 2, 'C' to 2, 'D' to 3, 'E' to 15,
            'F' to 2, 'G' to 2, 'H' to 2, 'I' to 8, 'J' to 1,
            'K' to 1, 'L' to 5, 'M' to 3, 'N' to 6, 'O' to 6,
            'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 6, 'T' to 6,
            'U' to 6, 'V' to 2, 'W' to 1, 'X' to 1, 'Y' to 1,
            'Z' to 1, '_' to 2 // Jokers
        )
    }
}