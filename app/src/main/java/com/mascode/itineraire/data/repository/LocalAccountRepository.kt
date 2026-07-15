package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.LocalAccountDao
import com.mascode.itineraire.data.local.entity.LocalAccountEntity

class LocalAccountRepository(private val accountDao: LocalAccountDao) {
    val account = accountDao.observe()

    suspend fun create(displayName: String) {
        val normalizedName = displayName.trim()
        require(normalizedName.length >= 2) { "Le nom doit contenir au moins deux caractères." }
        accountDao.insert(LocalAccountEntity(displayName = normalizedName))
    }
}
