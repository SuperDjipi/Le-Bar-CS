package club.djipi.lebarcs.data.local.dao

import androidx.room.*
import club.djipi.lebarcs.data.local.entity.GameEntity
import club.djipi.lebarcs.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGame(gameId: String): Flow<GameEntity?>
    
    @Query("SELECT * FROM games ORDER BY updatedAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)
    
    @Update
    suspend fun updateGame(game: GameEntity)
    
    @Delete
    suspend fun deleteGame(game: GameEntity)
    
    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: String)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE gameId = :gameId")
    fun getPlayersByGameId(gameId: String): Flow<List<PlayerEntity>>
    
    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayer(playerId: String): PlayerEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)
    
    @Update
    suspend fun updatePlayer(player: PlayerEntity)
    
    @Delete
    suspend fun deletePlayer(player: PlayerEntity)
    
    @Query("DELETE FROM players WHERE gameId = :gameId")
    suspend fun deletePlayersByGameId(gameId: String)
}
