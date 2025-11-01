package club.djipi.lebarcs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import club.djipi.lebarcs.data.local.dao.GameDao
import club.djipi.lebarcs.data.local.dao.PlayerDao
import club.djipi.lebarcs.data.local.entity.GameEntity
import club.djipi.lebarcs.data.local.entity.PlayerEntity

@Database(
    entities = [GameEntity::class, PlayerEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScrabbleDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun playerDao(): PlayerDao
    
    companion object {
        const val DATABASE_NAME = "scrabble_db"
    }
}
