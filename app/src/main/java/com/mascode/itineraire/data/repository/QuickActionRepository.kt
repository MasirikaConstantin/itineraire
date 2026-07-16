package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.QuickActionDao
import com.mascode.itineraire.data.local.entity.QuickActionEntity
import com.mascode.itineraire.domain.model.DayEventType
import java.time.Instant

class QuickActionRepository(private val quickActionDao: QuickActionDao) {
    val actions = quickActionDao.observeAll()

    suspend fun add(
        label: String,
        eventType: DayEventType,
        placeId: String? = null,
        notes: String? = null,
    ) {
        require(label.isNotBlank()) { "Le nom de l'action est obligatoire." }
        quickActionDao.insert(
            QuickActionEntity(
                label = label.trim(),
                eventType = eventType,
                placeId = placeId,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
                position = quickActionDao.nextPosition(),
            ),
        )
    }

    suspend fun delete(action: QuickActionEntity) = quickActionDao.delete(action)

    suspend fun update(
        action: QuickActionEntity,
        label: String,
        eventType: DayEventType,
        placeId: String?,
        notes: String?,
    ) {
        require(label.isNotBlank()) { "Le nom de l'action est obligatoire." }
        quickActionDao.update(
            action.copy(
                label = label.trim(),
                eventType = eventType,
                placeId = placeId,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = Instant.now(),
            ),
        )
    }
}
