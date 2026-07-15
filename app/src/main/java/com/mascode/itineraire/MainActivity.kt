package com.mascode.itineraire

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.AppViewModel
import com.mascode.itineraire.ui.navigation.ItineraireApp
import com.mascode.itineraire.ui.theme.ItineraireTheme
import androidx.lifecycle.ViewModelProvider

class MainActivity : FragmentActivity() {
    private lateinit var appViewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory((application as ItineraireApplication).container)
        appViewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]
        setContent {
            ItineraireTheme {
                ItineraireApp(factory, this, appViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) appViewModel.lock()
    }
}
