package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mascode.itineraire.data.local.entity.LocalAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAccountDao {
    @Query("SELECT * FROM local_account WHERE id = 1 LIMIT 1")
    fun observe(): Flow<LocalAccountEntity?>

    @Query("SELECT * FROM local_account WHERE id = 1 LIMIT 1")
    suspend fun get(): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: LocalAccountEntity)

    @Update
    suspend fun update(account: LocalAccountEntity)
}
