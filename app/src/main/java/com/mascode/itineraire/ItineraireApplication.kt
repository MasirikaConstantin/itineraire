package com.mascode.itineraire

import android.app.Application

class ItineraireApplication : Application() {
    val container by lazy { AppContainer(this) }
}
