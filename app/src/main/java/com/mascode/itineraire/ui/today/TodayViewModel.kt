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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
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
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TodayUiState> = selectedDate
        .flatMapLatest { date ->
            flow { emit(dayRepository.getOrCreate(date).id) }
                .flatMapLatest { id ->
                    combine(
                        dayRepository.observeEvents(id),
                        journeyRepository.observeForDay(id),
                        placeRepository.places,
                        errorMessage,
                    ) { events, journeys, places, error ->
                        TodayUiState(
                            isLoading = false,
                            selectedDate = date,
                            dayId = id,
                            events = events,
                            journeys = journeys,
                            places = places,
                            errorMessage = error,
                        )
                    }
                }
                .onStart { emit(TodayUiState(selectedDate = date)) }
                .catch { error ->
                    emit(
                        TodayUiState(
                            isLoading = false,
                            selectedDate = date,
                            errorMessage = error.message ?: "Impossible d'ouvrir cette journée.",
                        ),
                    )
                }
            }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun showPreviousDay() {
        selectedDate.update { it.minusDays(1) }
    }

    fun showNextDay() {
        selectedDate.update { date ->
            if (date.isBefore(LocalDate.now())) date.plusDays(1) else date
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date.coerceAtMost(LocalDate.now())
    }

    fun addEvent(type: DayEventType) = runAction { id -> dayRepository.addEvent(id, type) }

    fun startJourney(
        sourceId: String,
        destinationId: String,
        onStarted: (String) -> Unit = {},
    ) {
        val id = uiState.value.dayId ?: return
        viewModelScope.launch {
            runCatching { journeyRepository.start(id, sourceId, destinationId) }
                .onSuccess(onStarted)
                .onFailure { errorMessage.value = it.message ?: "Impossible de démarrer le trajet." }
        }
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
        val id = uiState.value.dayId ?: return
        viewModelScope.launch {
            runCatching { action(id) }
                .onFailure { errorMessage.value = it.message ?: "Une erreur est survenue." }
        }
    }
}
