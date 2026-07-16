package com.mascode.itineraire

import android.content.Context
import androidx.room.Room
import com.mascode.itineraire.data.local.ItineraireDatabase
import com.mascode.itineraire.data.local.DatabaseMigrations
import com.mascode.itineraire.data.repository.DayRepository
import com.mascode.itineraire.data.repository.AppSecurityRepository
import com.mascode.itineraire.data.repository.JourneyRepository
import com.mascode.itineraire.data.repository.LocalAccountRepository
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.data.repository.QuickActionRepository
import com.mascode.itineraire.data.repository.ThemeRepository
import com.mascode.itineraire.data.repository.BackupRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ItineraireDatabase::class.java,
        "itineraire.db",
    ).addMigrations(
        DatabaseMigrations.MIGRATION_1_2,
        DatabaseMigrations.MIGRATION_2_3,
        DatabaseMigrations.MIGRATION_3_4,
        DatabaseMigrations.MIGRATION_4_5,
    ).build()

    val dayRepository = DayRepository(database.dayLogDao(), database.dayEventDao())
    val placeRepository = PlaceRepository(database.placeDao())
    val journeyRepository = JourneyRepository(database.journeyDao())
    val localAccountRepository = LocalAccountRepository(database.localAccountDao())
    val appSecurityRepository = AppSecurityRepository(database.appSecurityDao())
    val themeRepository = ThemeRepository(context)
    val quickActionRepository = QuickActionRepository(database.quickActionDao())
    val backupRepository = BackupRepository(database, context.applicationContext.contentResolver)
}
