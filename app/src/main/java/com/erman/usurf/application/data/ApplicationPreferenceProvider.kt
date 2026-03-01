package com.erman.usurf.application.data

import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFERENCE_IS_FIRST_LAUNCH: String = "isFirstLaunch"

class ApplicationPreferenceProvider(private val preferences: SharedPreferences) {
    fun getIsFirstLaunch(): Boolean {
        return preferences.getBoolean(PREFERENCE_IS_FIRST_LAUNCH, true)
    }

    fun editIsFirstLaunch(isFirstTime: Boolean) {
        preferences.edit { putBoolean(PREFERENCE_IS_FIRST_LAUNCH, isFirstTime) }
    }
}
