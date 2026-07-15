package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.LocalAccountDao
import com.mascode.itineraire.data.local.entity.LocalAccountEntity

class LocalAccountRepository(private val accountDao: LocalAccountDao) {
    val account = accountDao.observe()

    suspend fun save(displayName: String) {
        val normalizedName = displayName.trim()
        require(normalizedName.length >= 2) { "Le nom doit contenir au moins deux caractères." }
        val current = accountDao.get()
        if (current == null) {
            accountDao.insert(LocalAccountEntity(displayName = normalizedName))
        } else {
            accountDao.update(current.copy(displayName = normalizedName, updatedAt = java.time.Instant.now()))
        }
    }

    suspend fun delete() = accountDao.delete()
}
