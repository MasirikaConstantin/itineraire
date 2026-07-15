package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mascode.itineraire.data.local.entity.QuickActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickActionDao {
    @Query("SELECT * FROM quick_actions ORDER BY position, label COLLATE NOCASE")
    fun observeAll(): Flow<List<QuickActionEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM quick_actions")
    suspend fun nextPosition(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(action: QuickActionEntity)

    @Delete
    suspend fun delete(action: QuickActionEntity)
}
