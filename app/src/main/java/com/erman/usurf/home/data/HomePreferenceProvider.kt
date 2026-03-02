package com.erman.usurf.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.home.utils.IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL
import com.erman.usurf.home.utils.KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class HomePreferenceProvider(private val preferences: SharedPreferences, private val context: Context) {
    fun editIsKitkatRemovableStorageWarningDisplayedPreference(choice: Boolean) {
        logd("editIsKitkatRemovableStorageWarningDisplayedPreference")
        preferences.edit { putBoolean(KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED, choice) }
    }

    fun getIsKitkatRemovableStorageWarningDisplayedPreference(): Boolean {
        logd("getIsKitkatRemovableStorageWarningDisplayedPreference")
        return context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(
                KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED,
                IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL,
            )
    }
}
