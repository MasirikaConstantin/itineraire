package com.mascode.itineraire.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.DayEventEntity
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.DayRepository
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.model.DayEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val dayId: String? = null,
    val events: List<DayEventEntity> = emptyList(),
    val journeys: List<JourneyEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val dayRepository: DayRepository,
    private val placeRepository: PlaceRepository,
    private val journeyRepository: JourneyRepository,
) : ViewModel() {
    private val dayId = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TodayUiState> = dayId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                dayRepository.observeEvents(id),
                journeyRepository.observeForDay(id),
                placeRepository.places,
                errorMessage,
            ) { events, journeys, places, error ->
                TodayUiState(
                    isLoading = false,
                    dayId = id,
                    events = events,
                    journeys = journeys,
                    places = places,
                    errorMessage = error,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        viewModelScope.launch {
            runCatching { dayRepository.getOrCreate(LocalDate.now()).id }
                .onSuccess { dayId.value = it }
                .onFailure { errorMessage.value = it.message ?: "Impossible d'ouvrir la journée." }
        }
    }

    fun addEvent(type: DayEventType) = runAction { id -> dayRepository.addEvent(id, type) }

    fun startJourney(sourceId: String, destinationId: String) = runAction { id ->
        journeyRepository.start(id, sourceId, destinationId)
    }

    fun finishJourney(journeyId: String) {
        viewModelScope.launch {
            runCatching { journeyRepository.finish(journeyId) }
                .onFailure { errorMessage.value = it.message ?: "Impossible de terminer le trajet." }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun runAction(action: suspend (String) -> Unit) {
        val id = dayId.value ?: return
        viewModelScope.launch {
            runCatching { action(id) }
                .onFailure { errorMessage.value = it.message ?: "Une erreur est survenue." }
        }
    }
}
