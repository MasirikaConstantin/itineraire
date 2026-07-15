package com.mascode.itineraire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys WHERE dayId = :dayId ORDER BY startedAt DESC")
    fun observeForDay(dayId: String): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journeys ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<JourneyEntity>>

    @Insert
    suspend fun insert(journey: JourneyEntity)

    @Query(
        """UPDATE journeys
           SET status = :status, endedAt = :endedAt, updatedAt = :endedAt
           WHERE id = :journeyId""",
    )
    suspend fun finish(journeyId: String, status: JourneyStatus, endedAt: Instant)

    @Insert
    suspend fun insertLeg(leg: JourneyLegEntity)

    @Query("SELECT * FROM journey_legs WHERE journeyId = :journeyId ORDER BY position")
    fun observeLegs(journeyId: String): Flow<List<JourneyLegEntity>>

    @Insert
    suspend fun insertObservation(observation: JourneyObservationEntity)
}
