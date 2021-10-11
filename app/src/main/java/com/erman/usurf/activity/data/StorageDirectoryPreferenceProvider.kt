package com.erman.usurf.activity.data

import android.content.SharedPreferences
import com.erman.usurf.activity.utils.KEY_INTENT_EXTCARD_CHOSEN_URI
import com.erman.usurf.utils.logd

class StorageDirectoryPreferenceProvider(private val preferences: SharedPreferences) {
    fun getChosenUri(): String? {
        logd("getChosenUri")
        return preferences.getString(KEY_INTENT_EXTCARD_CHOSEN_URI, "")
    }

    fun editChosenUri(treeUri: String) {
        logd("editChosenUri")
        preferences.edit().putString(KEY_INTENT_EXTCARD_CHOSEN_URI, treeUri).apply()
    }
}