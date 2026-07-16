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
    data class WidgetJourneyData(
        val journey: JourneyEntity,
        val activeLeg: JourneyLegEntity?,
        val nextPlannedLeg: PlannedJourneyLegEntity?,
    )
    data class PlannedLegInput(
        val sourcePlaceId: String,
        val destinationPlaceId: String,
        val transportMode: TransportMode,
    )
    val journeys = journeyDao.observeAll()

    fun observeById(journeyId: String) = journeyDao.observeById(journeyId)

    fun observeForDay(dayId: String) = journeyDao.observeForDay(dayId)

    fun observeLegs(journeyId: String) = journeyDao.observeLegs(journeyId)

    fun observeIncompleteLegs() = journeyDao.observeIncompleteLegs()

    fun observeFinishedLegs() = journeyDao.observeFinishedLegs()

    fun observeLeg(legId: String) = journeyDao.observeLeg(legId)

    fun observeObservations(journeyId: String) = journeyDao.observeObservations(journeyId)

    fun observePlannedLegs(journeyId: String) = journeyDao.observePlannedLegs(journeyId)

    suspend fun getWidgetJourneyData(): WidgetJourneyData? {
        val journey = journeyDao.findActiveJourney() ?: return null
        return WidgetJourneyData(
            journey = journey,
            activeLeg = journeyDao.findActiveLeg(journey.id),
            nextPlannedLeg = journeyDao.findNextPlannedLeg(journey.id),
        )
    }

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

    suspend fun reorderPlannedLeg(journeyId: String, fromIndex: Int, toIndex: Int) {
        val legs = journeyDao.getPlannedLegs(journeyId)
        require(legs.size >= 3) { "Il faut au moins deux étapes intermédiaires à réordonner." }
        val lastIndex = legs.lastIndex
        require(fromIndex in 0 until lastIndex && toIndex in 0 until lastIndex) {
            "La destination finale doit rester en dernière position."
        }
        if (fromIndex == toIndex) return

        val reordered = legs.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        val finalDestinationId = legs.last().destinationPlaceId
        check(reordered.last().destinationPlaceId == finalDestinationId) {
            "La destination finale doit rester en dernière position."
        }
        val firstSourceId = legs.first().sourcePlaceId
        var sourceId = firstSourceId
        val continuousLegs = reordered.mapIndexed { index, leg ->
            leg.copy(position = index, sourcePlaceId = sourceId).also {
                sourceId = it.destinationPlaceId
            }
        }
        journeyDao.replacePlannedLegs(journeyId, continuousLegs)
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

    suspend fun completeLegCost(legId: String, cost: Long) {
        require(cost >= 0) { "Le prix ne peut pas être négatif." }
        check(journeyDao.completeLegCost(legId, cost, Instant.now()) > 0) {
            "Ce tronçon est introuvable ou n'est pas terminé."
        }
    }

    suspend fun completeLegDetails(
        legId: String,
        sourcePlaceId: String?,
        destinationPlaceId: String?,
        transportMode: TransportMode,
        startedAt: Instant,
        endedAt: Instant,
        cost: Long,
        notes: String?,
    ) {
        require(sourcePlaceId == null || sourcePlaceId != destinationPlaceId) {
            "Le départ et l'arrivée du tronçon doivent être différents."
        }
        require(!endedAt.isBefore(startedAt)) {
            "L'heure d'arrivée doit suivre l'heure de départ."
        }
        require(cost >= 0) { "Le prix ne peut pas être négatif." }
        val leg = journeyDao.findLeg(legId) ?: error("Ce tronçon est introuvable.")
        check(leg.endedAt != null) { "Ce tronçon n'est pas encore terminé." }
        journeyDao.updateLeg(
            leg.copy(
                sourcePlaceId = sourcePlaceId,
                destinationPlaceId = destinationPlaceId,
                transportMode = transportMode,
                startedAt = startedAt,
                endedAt = endedAt,
                cost = cost,
                costPending = false,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = Instant.now(),
            ),
        )
    }

    suspend fun updateFinishedJourney(
        journeyId: String,
        sourcePlaceId: String,
        destinationPlaceId: String,
        startedAt: Instant,
        endedAt: Instant,
        notes: String?,
    ) {
        require(sourcePlaceId != destinationPlaceId) { "La source et la destination doivent être différentes." }
        require(!endedAt.isBefore(startedAt)) { "L'arrivée doit suivre le départ." }
        val journey = journeyDao.findById(journeyId) ?: error("Trajet introuvable.")
        check(journey.status != JourneyStatus.IN_PROGRESS) { "Un trajet en cours ne peut pas être modifié ici." }
        journeyDao.updateJourney(
            journey.copy(
                sourcePlaceId = sourcePlaceId,
                destinationPlaceId = destinationPlaceId,
                startedAt = startedAt,
                endedAt = endedAt,
                notes = notes?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = Instant.now(),
            ),
        )
    }

    suspend fun deleteFinishedJourney(journeyId: String) {
        val journey = journeyDao.findById(journeyId) ?: error("Trajet introuvable.")
        check(journey.status != JourneyStatus.IN_PROGRESS) { "Un trajet en cours ne peut pas être supprimé." }
        journeyDao.deleteJourney(journey)
    }

    suspend fun updateFinishedLeg(
        legId: String,
        sourcePlaceId: String?,
        destinationPlaceId: String?,
        transportMode: TransportMode,
        startedAt: Instant,
        endedAt: Instant,
        cost: Long,
        notes: String?,
    ) = completeLegDetails(
        legId, sourcePlaceId, destinationPlaceId, transportMode, startedAt, endedAt, cost, notes,
    )

    suspend fun deleteFinishedLeg(legId: String) {
        val leg = journeyDao.findLeg(legId) ?: error("Ce tronçon est introuvable.")
        check(leg.endedAt != null) { "Un tronçon en cours ne peut pas être supprimé." }
        journeyDao.deleteLeg(leg)
    }

    suspend fun finishActiveLegAndStartNext(journeyId: String) {
        val activeLeg = journeyDao.findActiveLeg(journeyId) ?: error("Aucun tronçon n'est en cours.")
        val nextPlanned = journeyDao.findNextPlannedLeg(journeyId)
        val now = Instant.now()
        val nextLeg = nextPlanned?.let { planned ->
            JourneyLegEntity(
                journeyId = journeyId,
                position = journeyDao.nextLegPosition(journeyId),
                sourcePlaceId = planned.sourcePlaceId,
                destinationPlaceId = planned.destinationPlaceId,
                transportMode = planned.transportMode,
                startedAt = now,
            )
        }
        check(
            journeyDao.finishActiveAndStartNext(
                activeLegId = activeLeg.id,
                endedAt = now,
                plannedLegId = nextPlanned?.id,
                nextLeg = nextLeg,
            ),
        ) { "Le tronçon actif est déjà terminé." }
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
