package com.mascode.itineraire.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "local_account")
data class LocalAccountEntity(
    @PrimaryKey val id: Int = SINGLE_ACCOUNT_ID,
    val displayName: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    companion object {
        const val SINGLE_ACCOUNT_ID = 1
    }
}
