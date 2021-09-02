package com.erman.usurf.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.application.MainApplication
import com.erman.usurf.home.utils.IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL
import com.erman.usurf.home.utils.KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class HomePreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    fun editIsKitkatRemovableStorageWarningDisplayedPreference(choice: Boolean) {
        logd("editIsKitkatRemovableStorageWarningDisplayedPreference")
        preferences.edit().putBoolean(KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED, choice).apply()
    }

    fun getIsKitkatRemovableStorageWarningDisplayedPreference(): Boolean {
        logd("getIsKitkatRemovableStorageWarningDisplayedPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED, IS_KITKAT_REMOVABLE_STORAGE_WARNING_DISPLAYED_DEF_VAL)
    }
}