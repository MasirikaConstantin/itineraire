package com.mascode.itineraire.data.repository

import android.app.UiModeManager
import android.content.Context
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
    private val mutableThemeMode = MutableStateFlow(readThemeMode())

    val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    fun setThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
        preferences.edit().putString(THEME_MODE_KEY, themeMode.name).apply()
        applySystemNightMode(applicationContext, themeMode)
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
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
            uiModeManager.setApplicationNightMode(nightMode)
        }
    }
}
