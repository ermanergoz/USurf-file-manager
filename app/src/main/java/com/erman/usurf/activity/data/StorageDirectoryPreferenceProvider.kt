package com.erman.usurf.activity.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.activity.utils.KEY_INTENT_EXTCARD_CHOSEN_URI
import com.erman.usurf.application.MainApplication
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class StorageDirectoryPreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    fun getChosenUri(): String? {
        logd("getChosenUri")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(KEY_INTENT_EXTCARD_CHOSEN_URI, "")
    }

    @SuppressLint("CommitPrefEdits")
    fun editChosenUri(treeUri: String) {
        logd("editChosenUri")
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(KEY_INTENT_EXTCARD_CHOSEN_URI, treeUri)
        preferencesEditor.apply()
    }
}