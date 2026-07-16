package com.mascode.itineraire.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.local.entity.PlannedJourneyLegEntity
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.geo.haversineDistanceMeters
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.TransportMode
import com.mascode.itineraire.ui.notification.JourneyNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class TransportSummary(
    val mode: TransportMode,
    val legCount: Int,
    val duration: Duration,
    val cost: Long,
    val distanceMeters: Double?,
    val locatedLegCount: Int,
)

enum class JourneyTimelineType { JOURNEY_START, LEG_START, OBSERVATION, LEG_END, JOURNEY_END }

data class JourneyTimelineItem(
    val instant: Instant,
    val type: JourneyTimelineType,
    val leg: JourneyLegEntity? = null,
    val observation: JourneyObservationEntity? = null,
)

data class ActiveJourneyUiState(
    val isLoading: Boolean = true,
    val journey: JourneyEntity? = null,
    val legs: List<JourneyLegEntity> = emptyList(),
    val observations: List<JourneyObservationEntity> = emptyList(),
    val plannedLegs: List<PlannedJourneyLegEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val errorMessage: String? = null,
) {
    val activeLeg: JourneyLegEntity?
        get() = legs.firstOrNull { it.endedAt == null }

    val totalCost: Long
        get() = legs.sumOf(JourneyLegEntity::cost)

    val totalDuration: Duration?
        get() = journey?.endedAt?.let { Duration.between(journey.startedAt, it) }

    val transportSummaries: List<TransportSummary>
        get() {
            val placesById = places.associateBy(PlaceEntity::id)
            return legs.filter { it.endedAt != null }
                .groupBy(JourneyLegEntity::transportMode)
                .map { (mode, modeLegs) ->
                    val distances = modeLegs.map { leg ->
                        leg.distanceMeters?.toDouble() ?: distanceBetween(
                            placesById[leg.sourcePlaceId],
                            placesById[leg.destinationPlaceId],
                        )
                    }
                    TransportSummary(
                        mode = mode,
                        legCount = modeLegs.size,
                        duration = modeLegs.fold(Duration.ZERO) { total, leg ->
                            total.plus(Duration.between(leg.startedAt, requireNotNull(leg.endedAt)))
                        },
                        cost = modeLegs.sumOf(JourneyLegEntity::cost),
                        distanceMeters = distances.filterNotNull().takeIf { it.isNotEmpty() }?.sum(),
                        locatedLegCount = distances.count { it != null },
                    )
                }
                .sortedByDescending(TransportSummary::duration)
        }

    val timeline: List<JourneyTimelineItem>
        get() {
            val currentJourney = journey ?: return emptyList()
            return buildList {
                add(JourneyTimelineItem(currentJourney.startedAt, JourneyTimelineType.JOURNEY_START))
                legs.forEach { leg ->
                    add(JourneyTimelineItem(leg.startedAt, JourneyTimelineType.LEG_START, leg = leg))
                    leg.endedAt?.let {
                        add(JourneyTimelineItem(it, JourneyTimelineType.LEG_END, leg = leg))
                    }
                }
                observations.forEach {
                    add(JourneyTimelineItem(it.occurredAt, JourneyTimelineType.OBSERVATION, observation = it))
                }
                currentJourney.endedAt?.let {
                    add(JourneyTimelineItem(it, JourneyTimelineType.JOURNEY_END))
                }
            }.sortedWith(compareBy(JourneyTimelineItem::instant, { timelinePriority(it.type) }))
        }

    val estimatedDistanceMeters: Double?
        get() {
            val currentJourney = journey ?: return null
            val placesById = places.associateBy(PlaceEntity::id)
            val legDistances = legs.map { leg ->
                leg.distanceMeters?.toDouble() ?: distanceBetween(
                    placesById[leg.sourcePlaceId],
                    placesById[leg.destinationPlaceId],
                )
            }
            if (legDistances.isNotEmpty() && legDistances.all { it != null }) {
                return legDistances.sumOf { it ?: 0.0 }
            }
            return distanceBetween(
                placesById[currentJourney.sourcePlaceId],
                placesById[currentJourney.destinationPlaceId],
            )
        }

    val hasCompleteLegDistanceCoverage: Boolean
        get() {
            if (legs.isEmpty()) return estimatedDistanceMeters != null
            val placesById = places.associateBy(PlaceEntity::id)
            return legs.all { leg ->
                leg.distanceMeters != null || distanceBetween(
                    placesById[leg.sourcePlaceId], placesById[leg.destinationPlaceId],
                ) != null
            }
        }

    val nextSourcePlaceId: String?
        get() = legs.lastOrNull()?.destinationPlaceId ?: journey?.sourcePlaceId

    val hasReachedFinalDestination: Boolean
        get() = legs.lastOrNull()?.destinationPlaceId == journey?.destinationPlaceId

    val canFinishJourney: Boolean
        get() = activeLeg == null && plannedLegs.isEmpty() && (legs.isEmpty() || hasReachedFinalDestination)
}

private fun timelinePriority(type: JourneyTimelineType): Int = when (type) {
    JourneyTimelineType.JOURNEY_START -> 0
    JourneyTimelineType.LEG_END -> 1
    JourneyTimelineType.OBSERVATION -> 2
    JourneyTimelineType.LEG_START -> 3
    JourneyTimelineType.JOURNEY_END -> 4
}

private fun distanceBetween(start: PlaceEntity?, end: PlaceEntity?): Double? {
    val startLatitude = start?.latitude ?: return null
    val startLongitude = start.longitude ?: return null
    val endLatitude = end?.latitude ?: return null
    val endLongitude = end.longitude ?: return null
    return haversineDistanceMeters(
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
    )
}

class ActiveJourneyViewModel(
    val journeyId: String,
    private val journeyRepository: JourneyRepository,
    placeRepository: PlaceRepository,
    private val journeyNotificationManager: JourneyNotificationManager,
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    private val journeyState = combine(
        journeyRepository.observeById(journeyId),
        journeyRepository.observeLegs(journeyId),
        journeyRepository.observeObservations(journeyId),
        placeRepository.places,
        errorMessage,
    ) { journey, legs, observations, places, error ->
        ActiveJourneyUiState(
            isLoading = false,
            journey = journey,
            legs = legs,
            observations = observations,
            places = places,
            errorMessage = error,
        )
    }

    val uiState: StateFlow<ActiveJourneyUiState> = combine(
        journeyState,
        journeyRepository.observePlannedLegs(journeyId),
    ) { state, plannedLegs ->
        state.copy(plannedLegs = plannedLegs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveJourneyUiState())

    fun startLeg(sourcePlaceId: String?, destinationPlaceId: String?, transportMode: TransportMode) {
        runAction("Impossible de démarrer ce tronçon.") {
            journeyRepository.startLeg(journeyId, sourcePlaceId, destinationPlaceId, transportMode)
        }
    }

    fun finishLeg(legId: String, cost: Long, notes: String?) {
        runAction("Impossible de terminer ce tronçon.") {
            journeyRepository.finishLeg(legId, cost, notes)
        }
    }

    fun completeLegCost(legId: String, cost: Long) {
        runAction("Impossible d'enregistrer le prix.") {
            journeyRepository.completeLegCost(legId, cost)
        }
    }

    fun startPlannedLeg(plannedLegId: String) {
        runAction("Impossible de commencer ce tronçon prévu.") {
            journeyRepository.startPlannedLeg(journeyId, plannedLegId)
        }
    }

    fun deletePlannedLeg(plannedLegId: String) {
        runAction("Impossible de retirer ce tronçon prévu.") {
            journeyRepository.deletePlannedLeg(plannedLegId)
        }
    }

    fun reorderPlannedLeg(fromIndex: Int, toIndex: Int, onReordered: () -> Unit) {
        viewModelScope.launch {
            runCatching { journeyRepository.reorderPlannedLeg(journeyId, fromIndex, toIndex) }
                .onSuccess {
                    journeyNotificationManager.synchronize()
                    onReordered()
                }
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Impossible de réordonner les tronçons."
                }
        }
    }

    fun addObservation(type: ObservationType, notes: String?) {
        runAction("Impossible d'ajouter cette observation.") {
            journeyRepository.addObservation(journeyId, uiState.value.activeLeg?.id, type, notes)
        }
    }

    fun finishJourney() {
        runAction("Impossible de terminer ce trajet.") { journeyRepository.finish(journeyId) }
    }

    fun cancelJourney() {
        runAction("Impossible d'annuler ce trajet.") { journeyRepository.cancel(journeyId) }
    }

    fun updateFinishedJourney(
        sourcePlaceId: String,
        destinationPlaceId: String,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
        notes: String?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                journeyRepository.updateFinishedJourney(
                    journeyId, sourcePlaceId, destinationPlaceId, startedAt, endedAt, notes,
                )
            }.onSuccess { onSaved() }
                .onFailure { errorMessage.value = it.message ?: "Impossible de modifier ce trajet." }
        }
    }

    fun deleteFinishedJourney(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { journeyRepository.deleteFinishedJourney(journeyId) }
                .onSuccess { onDeleted() }
                .onFailure { errorMessage.value = it.message ?: "Impossible de supprimer ce trajet." }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun runAction(fallbackMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { journeyNotificationManager.synchronize() }
                .onFailure { error -> errorMessage.value = error.message ?: fallbackMessage }
        }
    }
}
