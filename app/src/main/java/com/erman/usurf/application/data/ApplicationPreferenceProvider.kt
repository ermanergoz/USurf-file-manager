package com.erman.usurf.application.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.application.MainApplication
import com.erman.usurf.application.utils.PREFERENCE_IS_FIRST_LAUNCH
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class ApplicationPreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext
        .getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

    fun getIsFirstLaunch(): Boolean {
        logd("getIsFirstLaunch")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_IS_FIRST_LAUNCH, true)
    }

    fun editIsFirstLaunch(isFirstTime: Boolean) {
        logd("editIsFirstLaunch")
        preferences.edit().putBoolean(PREFERENCE_IS_FIRST_LAUNCH, isFirstTime).apply()
    }
}