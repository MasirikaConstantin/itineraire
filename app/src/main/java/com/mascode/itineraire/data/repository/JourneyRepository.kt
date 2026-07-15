package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.JourneyDao
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.Instant

class JourneyRepository(private val journeyDao: JourneyDao) {
    val journeys = journeyDao.observeAll()

    fun observeForDay(dayId: String) = journeyDao.observeForDay(dayId)

    suspend fun start(dayId: String, sourcePlaceId: String, destinationPlaceId: String) {
        require(sourcePlaceId != destinationPlaceId) { "La source et la destination doivent être différentes." }
        journeyDao.insert(
            JourneyEntity(
                dayId = dayId,
                sourcePlaceId = sourcePlaceId,
                destinationPlaceId = destinationPlaceId,
            ),
        )
    }

    suspend fun finish(journeyId: String) {
        journeyDao.finish(journeyId, JourneyStatus.COMPLETED, Instant.now())
    }
}
