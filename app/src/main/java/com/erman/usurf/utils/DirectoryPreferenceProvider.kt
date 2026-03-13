package com.erman.usurf.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.MainApplication

class DirectoryPreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    fun getChosenUri(): String? {
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(KEY_INTENT_EXTCARD_CHOSEN_URI, "")
    }

    @SuppressLint("CommitPrefEdits")
    fun editChosenUri(treeUri: String) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(KEY_INTENT_EXTCARD_CHOSEN_URI, treeUri)
        preferencesEditor.apply()
    }
}