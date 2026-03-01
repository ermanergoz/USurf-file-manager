package com.erman.usurf.home.data

import android.content.SharedPreferences
import androidx.core.content.edit

private const val IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL: Boolean = false
private const val KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED: String = "IsKitkatRemovableStorageWarningDisplayed"

class HomePreferenceProvider(private val preferences: SharedPreferences) {
    fun editIsKitkatRemovableStorageWarningDisplayedPreference(choice: Boolean) {
        preferences.edit { putBoolean(KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED, choice) }
    }

    fun getIsKitkatRemovableStorageWarningDisplayedPreference(): Boolean {
        return preferences.getBoolean(
            KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED,
            IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL,
        )
    }
}
