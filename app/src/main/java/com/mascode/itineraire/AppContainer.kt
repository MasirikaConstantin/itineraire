package com.mascode.itineraire

import android.content.Context
import androidx.room.Room
import com.mascode.itineraire.data.local.ItineraireDatabase
import com.mascode.itineraire.data.local.DatabaseMigrations
import com.mascode.itineraire.data.repository.DayRepository
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.LocalAccountRepository
import com.mascode.itineraire.data.repository.PlaceRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ItineraireDatabase::class.java,
        "itineraire.db",
    ).addMigrations(DatabaseMigrations.MIGRATION_1_2).build()

    val dayRepository = DayRepository(database.dayLogDao(), database.dayEventDao())
    val placeRepository = PlaceRepository(database.placeDao())
    val journeyRepository = JourneyRepository(database.journeyDao())
    val localAccountRepository = LocalAccountRepository(database.localAccountDao())
}
