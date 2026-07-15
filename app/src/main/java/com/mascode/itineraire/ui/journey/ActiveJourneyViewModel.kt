package com.mascode.itineraire.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.geo.haversineDistanceMeters
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.TransportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveJourneyUiState(
    val isLoading: Boolean = true,
    val journey: JourneyEntity? = null,
    val legs: List<JourneyLegEntity> = emptyList(),
    val observations: List<JourneyObservationEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val errorMessage: String? = null,
) {
    val activeLeg: JourneyLegEntity?
        get() = legs.firstOrNull { it.endedAt == null }

    val totalCost: Long
        get() = legs.sumOf(JourneyLegEntity::cost)

    val estimatedDistanceMeters: Double?
        get() {
            val currentJourney = journey ?: return null
            val placesById = places.associateBy(PlaceEntity::id)
            val legDistances = legs.map { leg ->
                distanceBetween(
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

    val nextSourcePlaceId: String?
        get() = legs.lastOrNull()?.destinationPlaceId ?: journey?.sourcePlaceId

    val hasReachedFinalDestination: Boolean
        get() = legs.lastOrNull()?.destinationPlaceId == journey?.destinationPlaceId
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
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ActiveJourneyUiState> = combine(
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

    fun clearError() {
        errorMessage.value = null
    }

    private fun runAction(fallbackMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onFailure { error -> errorMessage.value = error.message ?: fallbackMessage }
        }
    }
}
