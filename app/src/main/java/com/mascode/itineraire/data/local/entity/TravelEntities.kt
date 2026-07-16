package com.mascode.itineraire.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mascode.itineraire.domain.model.DayEventType
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.PlaceCategory
import com.mascode.itineraire.domain.model.TransportMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "day_logs",
    indices = [Index(value = ["date"], unique = true)],
)
data class DayLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "places",
    indices = [Index(value = ["name"], unique = true)],
)
data class PlaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: PlaceCategory = PlaceCategory.OTHER,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "day_events",
    foreignKeys = [
        ForeignKey(
            entity = DayLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("dayId"), Index("placeId"), Index("occurredAt")],
)
data class DayEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayId: String,
    val type: DayEventType,
    val occurredAt: Instant = Instant.now(),
    val placeId: String? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "journeys",
    foreignKeys = [
        ForeignKey(
            entity = DayLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePlaceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationPlaceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("dayId"), Index("sourcePlaceId"), Index("destinationPlaceId"), Index("startedAt")],
)
data class JourneyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayId: String,
    val sourcePlaceId: String,
    val destinationPlaceId: String,
    val startedAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
    val status: JourneyStatus = JourneyStatus.IN_PROGRESS,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "journey_legs",
    foreignKeys = [
        ForeignKey(
            entity = JourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePlaceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationPlaceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["journeyId", "position"], unique = true),
        Index("sourcePlaceId"),
        Index("destinationPlaceId"),
    ],
)
data class JourneyLegEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val journeyId: String,
    val position: Int,
    val sourcePlaceId: String? = null,
    val destinationPlaceId: String? = null,
    val transportMode: TransportMode,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val cost: Long = 0,
    val currency: String = "CDF",
    val distanceMeters: Long? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "planned_journey_legs",
    foreignKeys = [
        ForeignKey(
            entity = JourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePlaceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationPlaceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["journeyId", "position"], unique = true),
        Index("sourcePlaceId"),
        Index("destinationPlaceId"),
    ],
)
data class PlannedJourneyLegEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val journeyId: String,
    val position: Int,
    val sourcePlaceId: String,
    val destinationPlaceId: String,
    val transportMode: TransportMode,
    val createdAt: Instant = Instant.now(),
)

@Entity(
    tableName = "journey_observations",
    foreignKeys = [
        ForeignKey(
            entity = JourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = JourneyLegEntity::class,
            parentColumns = ["id"],
            childColumns = ["legId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("journeyId"), Index("legId"), Index("occurredAt")],
)
data class JourneyObservationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val journeyId: String,
    val legId: String? = null,
    val type: ObservationType,
    val occurredAt: Instant = Instant.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)
