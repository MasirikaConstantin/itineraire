package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlannedJourneyLegEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys WHERE id = :journeyId LIMIT 1")
    fun observeById(journeyId: String): Flow<JourneyEntity?>

    @Query("SELECT * FROM journeys WHERE dayId = :dayId ORDER BY startedAt DESC")
    fun observeForDay(dayId: String): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journeys ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<JourneyEntity>>

    @Insert
    suspend fun insert(journey: JourneyEntity)

    @Insert
    suspend fun insertPlannedLegs(legs: List<PlannedJourneyLegEntity>)

    @Query("SELECT * FROM planned_journey_legs WHERE journeyId = :journeyId ORDER BY position")
    fun observePlannedLegs(journeyId: String): Flow<List<PlannedJourneyLegEntity>>

    @Query("SELECT * FROM planned_journey_legs WHERE journeyId = :journeyId ORDER BY position")
    suspend fun getPlannedLegs(journeyId: String): List<PlannedJourneyLegEntity>

    @Query("SELECT * FROM planned_journey_legs WHERE id = :plannedLegId LIMIT 1")
    suspend fun findPlannedLeg(plannedLegId: String): PlannedJourneyLegEntity?

    @Query("SELECT * FROM planned_journey_legs WHERE journeyId = :journeyId ORDER BY position LIMIT 1")
    suspend fun findNextPlannedLeg(journeyId: String): PlannedJourneyLegEntity?

    @Query("DELETE FROM planned_journey_legs WHERE id = :plannedLegId")
    suspend fun deletePlannedLeg(plannedLegId: String): Int

    @Query("DELETE FROM planned_journey_legs WHERE journeyId = :journeyId AND position >= :position")
    suspend fun deletePlannedLegsFrom(journeyId: String, position: Int): Int

    @Query("DELETE FROM planned_journey_legs WHERE journeyId = :journeyId")
    suspend fun deleteAllPlannedLegs(journeyId: String)

    @Transaction
    suspend fun replacePlannedLegs(journeyId: String, legs: List<PlannedJourneyLegEntity>) {
        deleteAllPlannedLegs(journeyId)
        if (legs.isNotEmpty()) insertPlannedLegs(legs)
    }

    @Transaction
    suspend fun insertJourneyWithPlan(
        journey: JourneyEntity,
        plannedLegs: List<PlannedJourneyLegEntity>,
    ) {
        insert(journey)
        if (plannedLegs.isNotEmpty()) insertPlannedLegs(plannedLegs)
    }

    @Transaction
    suspend fun convertPlannedLegToActive(
        plannedLegId: String,
        leg: JourneyLegEntity,
    ): Boolean {
        insertLeg(leg)
        return deletePlannedLeg(plannedLegId) > 0
    }

    @Query(
        """UPDATE journeys
           SET status = :status, endedAt = :endedAt, updatedAt = :endedAt
           WHERE id = :journeyId""",
    )
    suspend fun updateStatus(journeyId: String, status: JourneyStatus, endedAt: Instant): Int

    @Insert
    suspend fun insertLeg(leg: JourneyLegEntity)

    @Query("SELECT * FROM journey_legs WHERE journeyId = :journeyId ORDER BY position")
    fun observeLegs(journeyId: String): Flow<List<JourneyLegEntity>>

    @Query("SELECT * FROM journey_legs WHERE journeyId = :journeyId AND endedAt IS NULL LIMIT 1")
    suspend fun findActiveLeg(journeyId: String): JourneyLegEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM journey_legs WHERE journeyId = :journeyId")
    suspend fun nextLegPosition(journeyId: String): Int

    @Query(
        """UPDATE journey_legs
           SET endedAt = :endedAt, cost = :cost, notes = :notes, updatedAt = :endedAt
           WHERE id = :legId AND endedAt IS NULL""",
    )
    suspend fun finishLeg(legId: String, endedAt: Instant, cost: Long, notes: String?): Int

    @Query(
        """UPDATE journey_legs
           SET endedAt = :endedAt, updatedAt = :endedAt
           WHERE journeyId = :journeyId AND endedAt IS NULL""",
    )
    suspend fun closeActiveLeg(journeyId: String, endedAt: Instant)

    @Insert
    suspend fun insertObservation(observation: JourneyObservationEntity)

    @Query("SELECT * FROM journey_observations WHERE journeyId = :journeyId ORDER BY occurredAt DESC")
    fun observeObservations(journeyId: String): Flow<List<JourneyObservationEntity>>
}
