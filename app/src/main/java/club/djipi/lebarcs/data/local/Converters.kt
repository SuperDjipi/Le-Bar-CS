package club.djipi.lebarcs.data.local

import androidx.room.TypeConverter

/**
 * Cette classe contient les convertisseurs de type pour la base de données Room.
 * Room l'utilisera pour savoir comment stocker et récupérer des types de données
 * complexes qui ne sont pas supportés nativement par SQLite.
 */
class Converters {

    /**
     * Convertit une liste de chaînes de caractères en une seule chaîne.
     * Room appellera cette fonction avant d'enregistrer les données en base.
     * @param playerIds La liste des IDs des joueurs.
     * @return Une chaîne unique avec les IDs séparés par une virgule.
     */
    @TypeConverter
    fun fromPlayerIdsList(playerIds: List<String>): String {
        return playerIds.joinToString(",")
    }

    /**
     * Convertit une chaîne unique en une liste de chaînes de caractères.
     * Room appellera cette fonction après avoir lu les données depuis la base.
     * @param playerIdsString La chaîne stockée en base (ex: "id1,id2,id3").
     * @return La liste des IDs des joueurs.
     */
    @TypeConverter
    fun toPlayerIdsList(playerIdsString: String): List<String> {
        // Gère le cas où la chaîne est vide pour ne pas créer une liste avec une chaîne vide.
        return if (playerIdsString.isEmpty()) {
            emptyList()
        } else {
            playerIdsString.split(",")
        }
    }

    // --- Ajoutez d'autres convertisseurs ici si nécessaire ---
    // Par exemple, pour convertir des dates, des objets JSON, etc.
    //
    // @TypeConverter
    // fun fromTimestamp(value: Long?): Date? {
    //     return value?.let { Date(it) }
    // }
    //
    // @TypeConverter
    // fun dateToTimestamp(date: Date?): Long? {
    //     return date?.time
    // }
}