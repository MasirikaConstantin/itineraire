package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mascode.itineraire.data.local.entity.DayLogEntity
import java.time.LocalDate

@Dao
interface DayLogDao {
    @Query("SELECT * FROM day_logs WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: LocalDate): DayLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(dayLog: DayLogEntity): Long
}
