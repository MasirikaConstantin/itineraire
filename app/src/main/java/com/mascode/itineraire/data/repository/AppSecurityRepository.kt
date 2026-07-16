package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.AppSecurityDao
import com.mascode.itineraire.data.local.entity.AppSecurityEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppSecurityRepository(private val securityDao: AppSecurityDao) {
    val biometricLockEnabled = securityDao.observe()
        .map { it?.biometricLockEnabled ?: false }
        .distinctUntilChanged()

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        securityDao.save(AppSecurityEntity(biometricLockEnabled = enabled))
    }

    suspend fun isBiometricLockEnabled(): Boolean = securityDao.get()?.biometricLockEnabled ?: false
}
