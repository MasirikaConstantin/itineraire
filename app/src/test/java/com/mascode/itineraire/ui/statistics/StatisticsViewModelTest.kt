package com.mascode.itineraire.ui.statistics

import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.TransportMode
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsViewModelTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 7, 16)
    private val source = PlaceEntity(id = "source", name = "Maison")
    private val work = PlaceEntity(id = "work", name = "Travail")
    private val church = PlaceEntity(id = "church", name = "Église")

    @Test
    fun `calcule les periodes et exclut le trajet actif`() {
        val todayJourney = journey("today", today, "work")
        val weekJourney = journey("week", LocalDate.of(2026, 7, 13), "work")
        val monthJourney = journey("month", LocalDate.of(2026, 7, 2), "church")
        val previousMonth = journey("old", LocalDate.of(2026, 6, 28), "church")
        val active = journey("active", today, "church", JourneyStatus.IN_PROGRESS)
        val journeys = listOf(todayJourney, weekJourney, monthJourney, previousMonth, active)
        val legs = listOf(
            leg("today", 1_000, TransportMode.TAXI),
            leg("week", 2_000, TransportMode.TAXI),
            leg("month", 3_000, TransportMode.BUS),
            leg("old", 4_000, TransportMode.WALK),
            leg("active", 99_000, TransportMode.TAXI),
        )

        val state = buildStatisticsState(journeys, legs, listOf(source, work, church), today)

        assertEquals(1_000, state.today.expense)
        assertEquals(3_000, state.week.expense)
        assertEquals(6_000, state.month.expense)
        assertEquals(10_000, state.all.expense)
        assertEquals(4, state.journeys.size)
        assertEquals(2, state.destinations(StatisticsPeriod.MONTH, today).first().visitCount)
        assertEquals(2, state.transports(StatisticsPeriod.MONTH, today).first().legCount)
    }

    private fun journey(
        id: String,
        date: LocalDate,
        destinationId: String,
        status: JourneyStatus = JourneyStatus.COMPLETED,
    ): JourneyEntity {
        val start = date.atTime(8, 0).atZone(zone).toInstant()
        return JourneyEntity(
            id = id,
            dayId = "day-$id",
            sourcePlaceId = source.id,
            destinationPlaceId = destinationId,
            startedAt = start,
            endedAt = if (status == JourneyStatus.IN_PROGRESS) null else start.plusSeconds(3_600),
            status = status,
        )
    }

    private fun leg(journeyId: String, cost: Long, mode: TransportMode): JourneyLegEntity {
        val start = today.atTime(8, 0).atZone(zone).toInstant()
        return JourneyLegEntity(
            id = "leg-$journeyId",
            journeyId = journeyId,
            position = 0,
            transportMode = mode,
            startedAt = start,
            endedAt = start.plusSeconds(1_800),
            cost = cost,
        )
    }
}
