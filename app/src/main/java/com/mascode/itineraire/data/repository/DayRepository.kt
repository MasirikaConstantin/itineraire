package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.DayEventDao
import com.mascode.itineraire.data.local.dao.DayLogDao
import com.mascode.itineraire.data.local.entity.DayEventEntity
import com.mascode.itineraire.data.local.entity.DayLogEntity
import com.mascode.itineraire.domain.model.DayEventType
import java.time.Instant
import java.time.LocalDate

class DayRepository(
    private val dayLogDao: DayLogDao,
    private val dayEventDao: DayEventDao,
) {
    suspend fun getOrCreate(date: LocalDate): DayLogEntity {
        dayLogDao.findByDate(date)?.let { return it }
        val newDay = DayLogEntity(date = date)
        dayLogDao.insert(newDay)
        return dayLogDao.findByDate(date) ?: newDay
    }

    fun observeEvents(dayId: String) = dayEventDao.observeForDay(dayId)

    suspend fun addEvent(
        dayId: String,
        type: DayEventType,
        occurredAt: Instant = Instant.now(),
        placeId: String? = null,
        notes: String? = null,
    ) {
        require(!occurredAt.isAfter(Instant.now())) { "L'heure de l'événement ne peut pas être future." }
        dayEventDao.insert(
            DayEventEntity(
                dayId = dayId,
                type = type,
                occurredAt = occurredAt,
                placeId = placeId,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    suspend fun updateEvent(
        eventId: String,
        type: DayEventType,
        occurredAt: Instant,
        placeId: String?,
        notes: String?,
    ) {
        require(!occurredAt.isAfter(Instant.now())) { "L'heure de l'événement ne peut pas être future." }
        val event = dayEventDao.findById(eventId) ?: error("Événement introuvable.")
        dayEventDao.update(
            event.copy(
                type = type,
                occurredAt = occurredAt,
                placeId = placeId,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = Instant.now(),
            ),
        )
    }

    suspend fun deleteEvent(eventId: String) {
        val event = dayEventDao.findById(eventId) ?: error("Événement introuvable.")
        dayEventDao.delete(event)
    }
}
