package com.erman.usurf.activity.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.activity.utils.KEY_INTENT_EXTCARD_CHOSEN_URI
import com.erman.usurf.application.MainApplication
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class StorageDirectoryPreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

    fun getChosenUri(): String? {
        logd("getChosenUri")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(KEY_INTENT_EXTCARD_CHOSEN_URI, "")
    }

    fun editChosenUri(treeUri: String) {
        logd("editChosenUri")
        preferences.edit().putString(KEY_INTENT_EXTCARD_CHOSEN_URI, treeUri).apply()
    }
}