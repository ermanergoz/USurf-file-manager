package com.erman.usurf.application.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.erman.usurf.utils.SHARED_PREF_FILE

private const val PREFERENCE_IS_FIRST_LAUNCH = "isFirstLaunch"

class ApplicationPreferenceProvider(private val context: Context) {
    private var preferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

    fun getIsFirstLaunch(): Boolean {
        return context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_IS_FIRST_LAUNCH, true)
    }

    fun editIsFirstLaunch(isFirstTime: Boolean) {
        preferences.edit { putBoolean(PREFERENCE_IS_FIRST_LAUNCH, isFirstTime) }
    }
}
