package com.mascode.itineraire.data.repository

import android.content.Context
import com.mascode.itineraire.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableThemeMode = MutableStateFlow(readThemeMode())

    val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    fun setThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
        preferences.edit().putString(THEME_MODE_KEY, themeMode.name).apply()
    }

    private fun readThemeMode(): ThemeMode {
        val storedValue = preferences.getString(THEME_MODE_KEY, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.SYSTEM
    }

    private companion object {
        const val PREFERENCES_NAME = "app_preferences"
        const val THEME_MODE_KEY = "theme_mode"
    }
}
