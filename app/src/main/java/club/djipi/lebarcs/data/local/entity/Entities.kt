package club.djipi.lebarcs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import club.djipi.lebarcs.domain.model.GameStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: String,
    val status: GameStatus,
    val currentPlayerIndex: Int,
    val tilesRemaining: Int,
    val boardState: String,      // JSON du plateau
    val movesHistory: String,    // JSON de l'historique
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey
    val id: String,
    val gameId: String,
    val name: String,
    val score: Int,
    val rack: String,           // JSON du chevalet
    val isActive: Boolean
)

// Convertisseurs pour Room
class Converters {
    @TypeConverter
    fun fromGameStatus(status: GameStatus): String = status.name
    
    @TypeConverter
    fun toGameStatus(status: String): GameStatus = GameStatus.valueOf(status)
    
    @TypeConverter
    fun fromList(list: List<String>): String = Json.encodeToString(list)
    
    @TypeConverter
    fun toList(json: String): List<String> = Json.decodeFromString(json)
}
