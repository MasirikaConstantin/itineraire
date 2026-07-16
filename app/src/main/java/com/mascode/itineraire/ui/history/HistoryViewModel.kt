package com.mascode.itineraire.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val journeys: List<JourneyEntity> = emptyList(),
    val placesById: Map<String, PlaceEntity> = emptyMap(),
    val incompleteLegs: List<JourneyLegEntity> = emptyList(),
)

class HistoryViewModel(
    journeyRepository: JourneyRepository,
    placeRepository: PlaceRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = combine(
        journeyRepository.journeys,
        placeRepository.places,
        journeyRepository.observeIncompleteLegs(),
    ) { journeys, places, incompleteLegs ->
        HistoryUiState(journeys, places.associateBy { it.id }, incompleteLegs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
