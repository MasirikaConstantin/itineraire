package com.mascode.itineraire.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mascode.itineraire.data.local.dao.DayEventDao
import com.mascode.itineraire.data.local.dao.DayLogDao
import com.mascode.itineraire.data.local.dao.JourneyDao
import com.mascode.itineraire.data.local.dao.PlaceDao
import com.mascode.itineraire.data.local.entity.DayEventEntity
import com.mascode.itineraire.data.local.entity.DayLogEntity
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity

@Database(
    entities = [
        DayLogEntity::class,
        PlaceEntity::class,
        DayEventEntity::class,
        JourneyEntity::class,
        JourneyLegEntity::class,
        JourneyObservationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ItineraireDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun placeDao(): PlaceDao
    abstract fun dayEventDao(): DayEventDao
    abstract fun journeyDao(): JourneyDao
}
