package com.mascode.itineraire

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.ThemeMode
import com.mascode.itineraire.ui.AppViewModel
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.navigation.ItineraireApp
import com.mascode.itineraire.ui.theme.ItineraireTheme

class MainActivity : FragmentActivity() {
    private lateinit var appViewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory((application as ItineraireApplication).container)
        appViewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDarkTheme by appViewModel.systemDarkTheme.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                )
            }
            ItineraireTheme(darkTheme = darkTheme) {
                ItineraireApp(
                    factory = factory,
                    activity = this,
                    viewModel = appViewModel,
                    themeMode = themeMode,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) appViewModel.lock()
    }

    override fun onResume() {
        super.onResume()
        appViewModel.refreshSystemTheme()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::appViewModel.isInitialized) appViewModel.refreshSystemTheme()
    }
}
