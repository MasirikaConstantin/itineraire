package com.mascode.itineraire.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mascode.itineraire.domain.model.DayEventType
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "quick_actions",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["label"], unique = true), Index("placeId")],
)
data class QuickActionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val eventType: DayEventType,
    val placeId: String? = null,
    val notes: String? = null,
    val position: Int,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
