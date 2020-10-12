package com.erman.usurf.preference.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.app.MainApplication
import com.erman.usurf.preference.utils.*
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class PreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext
        .getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

    fun editRootAccessPreference(choice: Boolean) {
        logd("editRootAccessPreference")
        preferences.edit().putBoolean(PREFERENCE_ROOT_ACCESS, choice).apply()
    }

    fun getRootAccessPreference(): Boolean {
        logd("getRootAccessPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_ROOT_ACCESS, ROOT_ACCESS_PREF_DEF_VAL)
    }

    fun editFileSortPreference(choice: String) {
        logd("editFileSortPreference")
        preferences.edit().putString(PREFERENCE_FILE_SORT, choice).apply()
    }

    fun getFileSortPreference(): String? {
        logd("getFileSortPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getString(PREFERENCE_FILE_SORT, DEFAULT_FILE_SORT_MODE)
    }

    fun editShowHiddenPreference(choice: Boolean) {
        logd("editShowHiddenPreference")
        preferences.edit().putBoolean(PREFERENCE_SHOW_HIDDEN, choice).apply()
    }

    fun getShowHiddenPreference(): Boolean {
        logd("getShowHiddenPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_SHOW_HIDDEN, SHOW_HIDDEN_PREF_DEF_VAL)
    }

    fun editAscendingOrderPreference(choice: Boolean) {
        logd("editAscendingOrderPreference")
        preferences.edit().putBoolean(PREFERENCE_DESCENDING_ORDER, false).putBoolean(PREFERENCE_ASCENDING_ORDER, choice).apply()
    }

    fun editDescendingOrderPreference(choice: Boolean) {
        logd("editDescendingOrderPreference")
        preferences.edit().putBoolean(PREFERENCE_ASCENDING_ORDER, false).putBoolean(PREFERENCE_DESCENDING_ORDER, choice).apply()
    }

    fun getDescendingOrderPreference(): Boolean {
        logd("getAscendingOrderPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREFERENCE_DESCENDING_ORDER, DESSCENDING_ORDER_PREF_DEF_VAL)
    }
}