package com.erman.usurf.preference.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.preference.utils.KEY_PREFERENCE_ROOT_ACCESS

private const val DESCENDING_ORDER_PREF_DEF_VAL: Boolean = false
private const val ROOT_ACCESS_PREF_DEF_VAL: Boolean = false
private const val FILE_SORT_MODE_DEF_VAL: String = "Sort by name"
private const val SHOW_HIDDEN_PREF_DEF_VAL: Boolean = false
private const val SHOW_THUMBNAILS_PREF_DEF_VAL: Boolean = true
private const val KEY_PREFERENCE_SHOW_HIDDEN: String = "showHidden"
private const val KEY_PREFERENCE_SHOW_THUMBNAILS: String = "showThumbnails"
private const val KEY_PREFERENCE_ASCENDING_ORDER: String = "ascendingOrder"
private const val KEY_PREFERENCE_DESCENDING_ORDER: String = "descendingOrder"
private const val KEY_PREFERENCE_FILE_SORT: String = "sortFileMode"

class PreferenceProvider(private val preferences: SharedPreferences) {
    fun editRootAccessPreference(choice: Boolean) {
        preferences.edit { putBoolean(KEY_PREFERENCE_ROOT_ACCESS, choice) }
    }

    fun getRootAccessPreference(): Boolean {
        return preferences.getBoolean(KEY_PREFERENCE_ROOT_ACCESS, ROOT_ACCESS_PREF_DEF_VAL)
    }

    fun editFileSortPreference(choice: String) {
        preferences.edit { putString(KEY_PREFERENCE_FILE_SORT, choice) }
    }

    fun getFileSortPreference(): String? {
        return preferences.getString(KEY_PREFERENCE_FILE_SORT, FILE_SORT_MODE_DEF_VAL)
    }

    fun editShowHiddenPreference(choice: Boolean) {
        preferences.edit { putBoolean(KEY_PREFERENCE_SHOW_HIDDEN, choice) }
    }

    fun getShowHiddenPreference(): Boolean {
        return preferences.getBoolean(KEY_PREFERENCE_SHOW_HIDDEN, SHOW_HIDDEN_PREF_DEF_VAL)
    }

    fun editShowThumbnailsPreference(choice: Boolean) {
        preferences.edit { putBoolean(KEY_PREFERENCE_SHOW_THUMBNAILS, choice) }
    }

    fun getShowThumbnailsPreference(): Boolean {
        return preferences.getBoolean(KEY_PREFERENCE_SHOW_THUMBNAILS, SHOW_THUMBNAILS_PREF_DEF_VAL)
    }

    fun editAscendingOrderPreference(choice: Boolean) {
        preferences.edit {
            putBoolean(KEY_PREFERENCE_DESCENDING_ORDER, false)
                .putBoolean(KEY_PREFERENCE_ASCENDING_ORDER, choice)
        }
    }

    fun editDescendingOrderPreference(choice: Boolean) {
        preferences.edit {
            putBoolean(KEY_PREFERENCE_ASCENDING_ORDER, false)
                .putBoolean(KEY_PREFERENCE_DESCENDING_ORDER, choice)
        }
    }

    fun getDescendingOrderPreference(): Boolean {
        return preferences.getBoolean(KEY_PREFERENCE_DESCENDING_ORDER, DESCENDING_ORDER_PREF_DEF_VAL)
    }
}
