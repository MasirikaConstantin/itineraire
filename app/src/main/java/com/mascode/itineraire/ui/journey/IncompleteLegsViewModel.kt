package com.mascode.itineraire.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.model.TransportMode
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IncompleteLegsUiState(
    val isLoading: Boolean = true,
    val legs: List<JourneyLegEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class IncompleteLegsViewModel(
    private val journeyRepository: JourneyRepository,
    placeRepository: PlaceRepository,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<IncompleteLegsUiState> = combine(
        journeyRepository.observeIncompleteLegs(),
        placeRepository.places,
        isSaving,
        errorMessage,
    ) { legs, places, saving, error ->
        IncompleteLegsUiState(false, legs, places, saving, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncompleteLegsUiState())

    fun complete(
        legId: String,
        sourcePlaceId: String?,
        destinationPlaceId: String?,
        transportMode: TransportMode,
        startedAt: Instant,
        endedAt: Instant,
        cost: Long,
        notes: String?,
        onSaved: () -> Unit,
    ) {
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            runCatching {
                journeyRepository.completeLegDetails(
                    legId = legId,
                    sourcePlaceId = sourcePlaceId,
                    destinationPlaceId = destinationPlaceId,
                    transportMode = transportMode,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    cost = cost,
                    notes = notes,
                )
            }.onSuccess {
                onSaved()
            }.onFailure {
                errorMessage.value = it.message ?: "Impossible d'enregistrer ce tronçon."
            }
            isSaving.value = false
        }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
