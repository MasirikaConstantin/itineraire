package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.JourneyDao
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.TransportMode
import java.time.Instant

class JourneyRepository(private val journeyDao: JourneyDao) {
    val journeys = journeyDao.observeAll()

    fun observeById(journeyId: String) = journeyDao.observeById(journeyId)

    fun observeForDay(dayId: String) = journeyDao.observeForDay(dayId)

    fun observeLegs(journeyId: String) = journeyDao.observeLegs(journeyId)

    fun observeObservations(journeyId: String) = journeyDao.observeObservations(journeyId)

    suspend fun start(dayId: String, sourcePlaceId: String, destinationPlaceId: String): String {
        require(sourcePlaceId != destinationPlaceId) { "La source et la destination doivent être différentes." }
        val journey = JourneyEntity(
            dayId = dayId,
            sourcePlaceId = sourcePlaceId,
            destinationPlaceId = destinationPlaceId,
        )
        journeyDao.insert(journey)
        return journey.id
    }

    suspend fun startLeg(
        journeyId: String,
        sourcePlaceId: String?,
        destinationPlaceId: String?,
        transportMode: TransportMode,
    ): String {
        require(sourcePlaceId == null || sourcePlaceId != destinationPlaceId) {
            "Le départ et l'arrivée du tronçon doivent être différents."
        }
        check(journeyDao.findActiveLeg(journeyId) == null) { "Terminez d'abord le tronçon en cours." }
        val leg = JourneyLegEntity(
            journeyId = journeyId,
            position = journeyDao.nextLegPosition(journeyId),
            sourcePlaceId = sourcePlaceId,
            destinationPlaceId = destinationPlaceId,
            transportMode = transportMode,
            startedAt = Instant.now(),
        )
        journeyDao.insertLeg(leg)
        return leg.id
    }

    suspend fun finishLeg(legId: String, cost: Long, notes: String?) {
        require(cost >= 0) { "Le prix ne peut pas être négatif." }
        check(
            journeyDao.finishLeg(
                legId = legId,
                endedAt = Instant.now(),
                cost = cost,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
            ) > 0,
        ) { "Ce tronçon est déjà terminé." }
    }

    suspend fun addObservation(
        journeyId: String,
        legId: String?,
        type: ObservationType,
        notes: String?,
    ) {
        journeyDao.insertObservation(
            JourneyObservationEntity(
                journeyId = journeyId,
                legId = legId,
                type = type,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    suspend fun finish(journeyId: String) {
        check(journeyDao.findActiveLeg(journeyId) == null) { "Terminez d'abord le tronçon en cours." }
        check(journeyDao.updateStatus(journeyId, JourneyStatus.COMPLETED, Instant.now()) > 0) {
            "Trajet introuvable."
        }
    }

    suspend fun cancel(journeyId: String) {
        val endedAt = Instant.now()
        journeyDao.closeActiveLeg(journeyId, endedAt)
        check(journeyDao.updateStatus(journeyId, JourneyStatus.CANCELLED, endedAt) > 0) {
            "Trajet introuvable."
        }
    }
}
