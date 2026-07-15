package com.mascode.itineraire.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "app_security")
data class AppSecurityEntity(
    @PrimaryKey val id: Int = SINGLE_SECURITY_SETTINGS_ID,
    val biometricLockEnabled: Boolean = false,
    val updatedAt: Instant = Instant.now(),
) {
    companion object {
        const val SINGLE_SECURITY_SETTINGS_ID = 1
    }
}
