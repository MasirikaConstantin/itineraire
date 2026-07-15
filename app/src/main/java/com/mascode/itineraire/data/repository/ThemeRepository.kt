package com.mascode.itineraire.data.repository

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import com.mascode.itineraire.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val uiModeManager = applicationContext.getSystemService(UiModeManager::class.java)
    private val mutableThemeMode = MutableStateFlow(readThemeMode())
    private val mutableSystemDarkTheme = MutableStateFlow(isSystemDarkThemeActive(uiModeManager))
    private val configurationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshSystemTheme()
        }
    }

    val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()
    val systemDarkTheme: StateFlow<Boolean> = mutableSystemDarkTheme.asStateFlow()

    init {
        applicationContext.registerReceiver(
            configurationReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    fun setThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
        preferences.edit().putString(THEME_MODE_KEY, themeMode.name).apply()
        applySystemNightMode(applicationContext, themeMode)
        refreshSystemTheme()
    }

    fun refreshSystemTheme() {
        val darkTheme = isSystemDarkThemeActive(uiModeManager)
        mutableSystemDarkTheme.value = darkTheme
        if (mutableThemeMode.value == ThemeMode.SYSTEM) {
            uiModeManager.setApplicationNightMode(
                if (darkTheme) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO,
            )
        }
    }

    private fun readThemeMode(): ThemeMode {
        val storedValue = preferences.getString(THEME_MODE_KEY, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.SYSTEM
    }

    companion object {
        private const val PREFERENCES_NAME = "app_preferences"
        private const val THEME_MODE_KEY = "theme_mode"

        fun synchronizeStoredTheme(context: Context) {
            val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val storedValue = preferences.getString(THEME_MODE_KEY, null)
            val themeMode = ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.SYSTEM
            applySystemNightMode(context, themeMode)
        }

        private fun applySystemNightMode(context: Context, themeMode: ThemeMode) {
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            val nightMode = when (themeMode) {
                ThemeMode.SYSTEM -> if (isSystemDarkThemeActive(uiModeManager)) {
                    UiModeManager.MODE_NIGHT_YES
                } else {
                    UiModeManager.MODE_NIGHT_NO
                }
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
            uiModeManager.setApplicationNightMode(nightMode)
        }
    }
}

private fun isSystemDarkThemeActive(uiModeManager: UiModeManager): Boolean =
    when (uiModeManager.nightMode) {
        UiModeManager.MODE_NIGHT_YES -> true
        UiModeManager.MODE_NIGHT_NO -> false
        else -> Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }
