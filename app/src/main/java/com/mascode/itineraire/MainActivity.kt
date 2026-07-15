package com.mascode.itineraire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.navigation.ItineraireApp
import com.mascode.itineraire.ui.theme.ItineraireTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory((application as ItineraireApplication).container)
        setContent {
            ItineraireTheme {
                ItineraireApp(factory)
            }
        }
    }
}
