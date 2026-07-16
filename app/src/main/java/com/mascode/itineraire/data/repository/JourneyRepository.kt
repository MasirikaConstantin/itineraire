package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.JourneyDao
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlannedJourneyLegEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.TransportMode
import java.time.Instant

class JourneyRepository(private val journeyDao: JourneyDao) {
    data class PlannedLegInput(
        val sourcePlaceId: String,
        val destinationPlaceId: String,
        val transportMode: TransportMode,
    )
    val journeys = journeyDao.observeAll()

    fun observeById(journeyId: String) = journeyDao.observeById(journeyId)

    fun observeForDay(dayId: String) = journeyDao.observeForDay(dayId)

    fun observeLegs(journeyId: String) = journeyDao.observeLegs(journeyId)

    fun observeObservations(journeyId: String) = journeyDao.observeObservations(journeyId)

    fun observePlannedLegs(journeyId: String) = journeyDao.observePlannedLegs(journeyId)

    suspend fun start(
        dayId: String,
        sourcePlaceId: String,
        destinationPlaceId: String,
        plannedLegs: List<PlannedLegInput> = emptyList(),
    ): String {
        require(sourcePlaceId != destinationPlaceId) { "La source et la destination doivent être différentes." }
        validatePlan(sourcePlaceId, destinationPlaceId, plannedLegs)
        val journey = JourneyEntity(
            dayId = dayId,
            sourcePlaceId = sourcePlaceId,
            destinationPlaceId = destinationPlaceId,
        )
        journeyDao.insertJourneyWithPlan(
            journey,
            plannedLegs.mapIndexed { index, leg ->
                PlannedJourneyLegEntity(
                    journeyId = journey.id,
                    position = index,
                    sourcePlaceId = leg.sourcePlaceId,
                    destinationPlaceId = leg.destinationPlaceId,
                    transportMode = leg.transportMode,
                )
            },
        )
        return journey.id
    }

    suspend fun startPlannedLeg(journeyId: String, plannedLegId: String): String {
        check(journeyDao.findActiveLeg(journeyId) == null) { "Terminez d'abord le tronçon en cours." }
        val plannedLeg = journeyDao.findPlannedLeg(plannedLegId) ?: error("Ce tronçon prévu est introuvable.")
        check(plannedLeg.journeyId == journeyId) { "Ce tronçon n'appartient pas à ce trajet." }
        check(journeyDao.findNextPlannedLeg(journeyId)?.id == plannedLegId) {
            "Commencez d'abord le tronçon prévu précédent."
        }
        val leg = JourneyLegEntity(
            journeyId = journeyId,
            position = journeyDao.nextLegPosition(journeyId),
            sourcePlaceId = plannedLeg.sourcePlaceId,
            destinationPlaceId = plannedLeg.destinationPlaceId,
            transportMode = plannedLeg.transportMode,
            startedAt = Instant.now(),
        )
        check(journeyDao.convertPlannedLegToActive(plannedLegId, leg)) {
            "Ce tronçon prévu est introuvable."
        }
        return leg.id
    }

    suspend fun deletePlannedLeg(plannedLegId: String) {
        val plannedLeg = journeyDao.findPlannedLeg(plannedLegId) ?: error("Ce tronçon prévu est introuvable.")
        journeyDao.deletePlannedLegsFrom(plannedLeg.journeyId, plannedLeg.position)
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

    private fun validatePlan(
        sourcePlaceId: String,
        destinationPlaceId: String,
        plannedLegs: List<PlannedLegInput>,
    ) {
        if (plannedLegs.isEmpty()) return
        require(plannedLegs.first().sourcePlaceId == sourcePlaceId) {
            "Le premier tronçon doit commencer au lieu de départ."
        }
        plannedLegs.forEachIndexed { index, leg ->
            require(leg.sourcePlaceId != leg.destinationPlaceId) {
                "Le départ et l'arrivée d'un tronçon doivent être différents."
            }
            if (index > 0) {
                require(plannedLegs[index - 1].destinationPlaceId == leg.sourcePlaceId) {
                    "Les tronçons prévus doivent se suivre dans l'ordre."
                }
            }
        }
        require(plannedLegs.last().destinationPlaceId == destinationPlaceId) {
            "Le dernier tronçon doit atteindre la destination finale."
        }
    }
}
