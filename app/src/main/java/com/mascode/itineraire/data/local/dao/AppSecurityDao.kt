package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mascode.itineraire.data.local.entity.AppSecurityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSecurityDao {
    @Query("SELECT * FROM app_security WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSecurityEntity?>

    @Query("SELECT * FROM app_security WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSecurityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSecurityEntity)
}
