package com.erman.usurf.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.utils.SHARED_PREF_FILE

private const val IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL: Boolean = false
private const val KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED: String = "IsKitkatRemovableStorageWarningDisplayed"

class HomePreferenceProvider(private val preferences: SharedPreferences, private val context: Context) {
    fun editIsKitkatRemovableStorageWarningDisplayedPreference(choice: Boolean) {
        preferences.edit { putBoolean(KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED, choice) }
    }

    fun getIsKitkatRemovableStorageWarningDisplayedPreference(): Boolean {
        return context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(
                KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED,
                IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL,
            )
    }
}
