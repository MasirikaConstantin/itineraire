package com.mascode.itineraire.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.DayEventEntity
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.local.entity.QuickActionEntity
import com.mascode.itineraire.data.repository.DayRepository
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.data.repository.QuickActionRepository
import com.mascode.itineraire.domain.model.DayEventType
import com.mascode.itineraire.data.repository.JourneyRepository.PlannedLegInput
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
import java.time.Instant
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val dayId: String? = null,
    val events: List<DayEventEntity> = emptyList(),
    val journeys: List<JourneyEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val quickActions: List<QuickActionEntity> = emptyList(),
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val dayRepository: DayRepository,
    private val placeRepository: PlaceRepository,
    private val journeyRepository: JourneyRepository,
    private val quickActionRepository: QuickActionRepository,
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
                        quickActionRepository.actions,
                        errorMessage,
                    ) { events, journeys, places, quickActions, error ->
                        TodayUiState(
                            isLoading = false,
                            selectedDate = date,
                            dayId = id,
                            events = events,
                            journeys = journeys,
                            places = places,
                            quickActions = quickActions,
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

    fun addEvent(
        type: DayEventType,
        occurredAt: Instant = Instant.now(),
        placeId: String? = null,
        notes: String? = null,
        onSaved: () -> Unit = {},
    ) {
        val id = uiState.value.dayId ?: return
        viewModelScope.launch {
            runCatching { dayRepository.addEvent(id, type, occurredAt, placeId, notes) }
                .onSuccess { onSaved() }
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Impossible d'enregistrer l'événement."
                }
        }
    }

    fun startJourney(
        sourceId: String,
        destinationId: String,
        plannedLegs: List<PlannedLegInput> = emptyList(),
        onStarted: (String) -> Unit = {},
    ) {
        val id = uiState.value.dayId ?: return
        viewModelScope.launch {
            runCatching { journeyRepository.start(id, sourceId, destinationId, plannedLegs) }
                .onSuccess(onStarted)
                .onFailure { errorMessage.value = it.message ?: "Impossible de démarrer le trajet." }
        }
    }

    fun runQuickAction(action: QuickActionEntity) {
        addEvent(
            type = action.eventType,
            placeId = action.placeId,
            notes = action.notes,
        )
    }

    fun addQuickAction(
        label: String,
        eventType: DayEventType,
        placeId: String?,
        notes: String?,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { quickActionRepository.add(label, eventType, placeId, notes) }
                .onSuccess { onSaved() }
                .onFailure {
                    errorMessage.value = "Cette action existe déjà ou n'est pas valide."
                }
        }
    }

    fun deleteQuickAction(action: QuickActionEntity) {
        viewModelScope.launch {
            runCatching { quickActionRepository.delete(action) }
                .onFailure { errorMessage.value = "Impossible de supprimer cette action." }
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

}
