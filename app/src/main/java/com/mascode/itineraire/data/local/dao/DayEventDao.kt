package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mascode.itineraire.data.local.entity.DayEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayEventDao {
    @Query("SELECT * FROM day_events WHERE dayId = :dayId ORDER BY occurredAt DESC")
    fun observeForDay(dayId: String): Flow<List<DayEventEntity>>

    @Query("SELECT * FROM day_events WHERE id = :eventId LIMIT 1")
    suspend fun findById(eventId: String): DayEventEntity?

    @Insert
    suspend fun insert(event: DayEventEntity)

    @Update
    suspend fun update(event: DayEventEntity)

    @Delete
    suspend fun delete(event: DayEventEntity)
}
