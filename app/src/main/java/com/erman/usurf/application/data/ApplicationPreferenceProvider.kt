package com.erman.usurf.application.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.erman.usurf.application.MainApplication
import com.erman.usurf.application.utils.PREFERENCE_IS_FIRST_LAUNCH
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class ApplicationPreferenceProvider(private val context: Context) {
    private var preferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

    fun getIsFirstLaunch(): Boolean {
        logd("getIsFirstLaunch")
        return context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_IS_FIRST_LAUNCH, true)
    }

    fun editIsFirstLaunch(isFirstTime: Boolean) {
        logd("editIsFirstLaunch")
        preferences.edit { putBoolean(PREFERENCE_IS_FIRST_LAUNCH, isFirstTime) }
    }
}
