package com.erman.usurf.storage.data

import android.content.SharedPreferences
import androidx.core.content.edit

private const val KEY_INTENT_EXTCARD_CHOSEN_URI: String = "extSdCardChosenUri"

class StorageDirectoryPreferenceProvider(private val preferences: SharedPreferences) {
    fun getChosenUri(): String? {
        return preferences.getString(KEY_INTENT_EXTCARD_CHOSEN_URI, "")
    }

    fun editChosenUri(treeUri: String) {
        preferences.edit { putString(KEY_INTENT_EXTCARD_CHOSEN_URI, treeUri) }
    }
}
