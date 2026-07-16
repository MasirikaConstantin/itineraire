package com.mascode.itineraire.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.TransportMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class StatisticsPeriod(val label: String) {
    DAY("Aujourd'hui"),
    WEEK("Cette semaine"),
    MONTH("Ce mois"),
    ALL("Tout"),
}

data class PeriodMetric(
    val expense: Long = 0,
    val duration: Duration = Duration.ZERO,
    val journeyCount: Int = 0,
)

data class TransportStatistic(
    val mode: TransportMode,
    val legCount: Int,
    val duration: Duration,
    val expense: Long,
)

data class DestinationStatistic(
    val placeId: String,
    val name: String,
    val visitCount: Int,
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val today: PeriodMetric = PeriodMetric(),
    val week: PeriodMetric = PeriodMetric(),
    val month: PeriodMetric = PeriodMetric(),
    val all: PeriodMetric = PeriodMetric(),
    val journeys: List<JourneyEntity> = emptyList(),
    val legs: List<JourneyLegEntity> = emptyList(),
    val placesById: Map<String, PlaceEntity> = emptyMap(),
) {
    fun metric(period: StatisticsPeriod): PeriodMetric = when (period) {
        StatisticsPeriod.DAY -> today
        StatisticsPeriod.WEEK -> week
        StatisticsPeriod.MONTH -> month
        StatisticsPeriod.ALL -> all
    }

    fun transports(period: StatisticsPeriod, todayDate: LocalDate = LocalDate.now()): List<TransportStatistic> {
        val journeyIds = filteredJourneys(period, todayDate).mapTo(mutableSetOf(), JourneyEntity::id)
        return legs.filter { it.journeyId in journeyIds }
            .groupBy(JourneyLegEntity::transportMode)
            .map { (mode, modeLegs) ->
                TransportStatistic(
                    mode = mode,
                    legCount = modeLegs.size,
                    duration = modeLegs.fold(Duration.ZERO) { total, leg ->
                        total.plus(Duration.between(leg.startedAt, requireNotNull(leg.endedAt)))
                    },
                    expense = modeLegs.sumOf(JourneyLegEntity::cost),
                )
            }
            .sortedWith(compareByDescending<TransportStatistic> { it.legCount }.thenByDescending { it.duration })
    }

    fun destinations(period: StatisticsPeriod, todayDate: LocalDate = LocalDate.now()): List<DestinationStatistic> =
        filteredJourneys(period, todayDate)
            .groupingBy(JourneyEntity::destinationPlaceId)
            .eachCount()
            .map { (placeId, count) ->
                DestinationStatistic(placeId, placesById[placeId]?.name ?: "Lieu inconnu", count)
            }
            .sortedWith(compareByDescending<DestinationStatistic> { it.visitCount }.thenBy { it.name })

    private fun filteredJourneys(period: StatisticsPeriod, todayDate: LocalDate): List<JourneyEntity> =
        journeys.filter { journey -> period.contains(journey.localDate(), todayDate) }
}

class StatisticsViewModel(
    journeyRepository: JourneyRepository,
    placeRepository: PlaceRepository,
) : ViewModel() {
    val uiState: StateFlow<StatisticsUiState> = combine(
        journeyRepository.journeys,
        journeyRepository.observeFinishedLegs(),
        placeRepository.places,
    ) { allJourneys, legs, places ->
        buildStatisticsState(allJourneys, legs, places, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
}

internal fun buildStatisticsState(
    allJourneys: List<JourneyEntity>,
    legs: List<JourneyLegEntity>,
    places: List<PlaceEntity>,
    today: LocalDate,
): StatisticsUiState {
    val journeys = allJourneys.filter { it.status != JourneyStatus.IN_PROGRESS && it.endedAt != null }
    val journeyIds = journeys.mapTo(mutableSetOf(), JourneyEntity::id)
    return StatisticsUiState(
        isLoading = false,
        today = calculateMetric(journeys, legs, StatisticsPeriod.DAY, today),
        week = calculateMetric(journeys, legs, StatisticsPeriod.WEEK, today),
        month = calculateMetric(journeys, legs, StatisticsPeriod.MONTH, today),
        all = calculateMetric(journeys, legs, StatisticsPeriod.ALL, today),
        journeys = journeys,
        legs = legs.filter { leg -> leg.journeyId in journeyIds },
        placesById = places.associateBy(PlaceEntity::id),
    )
}

private fun calculateMetric(
    journeys: List<JourneyEntity>,
    legs: List<JourneyLegEntity>,
    period: StatisticsPeriod,
    today: LocalDate,
): PeriodMetric {
    val filteredJourneys = journeys.filter { period.contains(it.localDate(), today) }
    val ids = filteredJourneys.mapTo(mutableSetOf(), JourneyEntity::id)
    return PeriodMetric(
        expense = legs.filter { it.journeyId in ids }.sumOf(JourneyLegEntity::cost),
        duration = filteredJourneys.fold(Duration.ZERO) { total, journey ->
            total.plus(Duration.between(journey.startedAt, requireNotNull(journey.endedAt)))
        },
        journeyCount = filteredJourneys.size,
    )
}

private fun StatisticsPeriod.contains(date: LocalDate, today: LocalDate): Boolean = when (this) {
    StatisticsPeriod.DAY -> date == today
    StatisticsPeriod.WEEK -> {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        !date.isBefore(start) && !date.isAfter(today)
    }
    StatisticsPeriod.MONTH -> date.year == today.year && date.month == today.month
    StatisticsPeriod.ALL -> true
}

private fun JourneyEntity.localDate(): LocalDate =
    startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
