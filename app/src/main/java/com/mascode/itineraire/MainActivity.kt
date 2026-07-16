package com.mascode.itineraire

import android.Manifest
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.mascode.itineraire.domain.model.ThemeMode
import com.mascode.itineraire.ui.AppViewModel
import com.mascode.itineraire.ui.AppViewModelFactory
import com.mascode.itineraire.ui.navigation.ItineraireApp
import com.mascode.itineraire.ui.theme.ItineraireTheme
import com.mascode.itineraire.ui.widget.updateJourneyWidgets
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var appViewModel: AppViewModel
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        lifecycleScope.launch {
            applicationContainer.journeyNotificationManager.synchronize()
        }
    }

    private val applicationContainer
        get() = (application as ItineraireApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory(applicationContainer)
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
        lifecycleScope.launch {
            updateJourneyWidgets(this@MainActivity)
            applicationContainer.journeyNotificationManager.synchronize()
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.refreshSystemTheme()
        lifecycleScope.launch {
            requestNotificationPermissionOnce()
            updateJourneyWidgets(this@MainActivity)
            applicationContainer.journeyNotificationManager.synchronize()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::appViewModel.isInitialized) appViewModel.refreshSystemTheme()
    }

    private fun requestNotificationPermissionOnce() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        val preferences = getSharedPreferences(NOTIFICATION_PREFERENCES, MODE_PRIVATE)
        if (preferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) return
        preferences.edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val NOTIFICATION_PREFERENCES = "journey_notifications"
        const val NOTIFICATION_PERMISSION_REQUESTED = "permission_requested"
    }
}
