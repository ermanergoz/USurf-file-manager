package com.erman.usurf.preference.data

import android.content.SharedPreferences
import com.erman.usurf.preference.utils.*
import com.erman.usurf.utils.logd

class PreferenceProvider(private val preferences: SharedPreferences) {
    fun editRootAccessPreference(choice: Boolean) {
        logd("editRootAccessPreference")
        preferences.edit().putBoolean(KEY_PREFERENCE_ROOT_ACCESS, choice).apply()
    }

    fun getRootAccessPreference(): Boolean {
        logd("getRootAccessPreference")
        return preferences.getBoolean(KEY_PREFERENCE_ROOT_ACCESS, ROOT_ACCESS_PREF_DEF_VAL)
    }

    fun editFileSortPreference(choice: String) {
        logd("editFileSortPreference")
        preferences.edit().putString(KEY_PREFERENCE_FILE_SORT, choice).apply()
    }

    fun getFileSortPreference(): String? {
        logd("getFileSortPreference")
        return preferences.getString(KEY_PREFERENCE_FILE_SORT, FILE_SORT_MODE_DEF_VAL)
    }

    fun editShowHiddenPreference(choice: Boolean) {
        logd("editShowHiddenPreference")
        preferences.edit().putBoolean(KEY_PREFERENCE_SHOW_HIDDEN, choice).apply()
    }

    fun getShowHiddenPreference(): Boolean {
        logd("getShowHiddenPreference")
        return preferences.getBoolean(KEY_PREFERENCE_SHOW_HIDDEN, SHOW_HIDDEN_PREF_DEF_VAL)
    }

    fun editShowThumbnailsPreference(choice: Boolean) {
        logd("editShowThumbnailsPreference")
        preferences.edit().putBoolean(KEY_PREFERENCE_SHOW_THUMBNAILS, choice).apply()
    }

    fun getShowThumbnailsPreference(): Boolean {
        logd("getShowThumbnailsPreference")
        return preferences.getBoolean(KEY_PREFERENCE_SHOW_THUMBNAILS, SHOW_THUMBNAILS_PREF_DEF_VAL)
    }

    fun editAscendingOrderPreference(choice: Boolean) {
        logd("editAscendingOrderPreference")
        preferences.edit().putBoolean(KEY_PREFERENCE_DESCENDING_ORDER, false).putBoolean(KEY_PREFERENCE_ASCENDING_ORDER, choice).apply()
    }

    fun editDescendingOrderPreference(choice: Boolean) {
        logd("editDescendingOrderPreference")
        preferences.edit().putBoolean(KEY_PREFERENCE_ASCENDING_ORDER, false).putBoolean(KEY_PREFERENCE_DESCENDING_ORDER, choice).apply()
    }

    fun getDescendingOrderPreference(): Boolean {
        logd("getAscendingOrderPreference")
        return preferences.getBoolean(KEY_PREFERENCE_DESCENDING_ORDER, DESCENDING_ORDER_PREF_DEF_VAL)
    }
}