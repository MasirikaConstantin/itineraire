package com.mascode.itineraire

import android.app.Application
import com.mascode.itineraire.data.repository.ThemeRepository

class ItineraireApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeRepository.synchronizeStoredTheme(this)
    }
}
