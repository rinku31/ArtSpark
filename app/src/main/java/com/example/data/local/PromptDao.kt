package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<PromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PromptEntity): Long

    @Update
    suspend fun update(entity: PromptEntity)

    @Delete
    suspend fun delete(entity: PromptEntity)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE prompts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM prompts WHERE isFavorite = 0")
    suspend fun clearHistoryNonFavorites()

    @Query("DELETE FROM prompts")
    suspend fun clearAll()
}
