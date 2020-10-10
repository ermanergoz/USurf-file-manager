package com.erman.usurf.preference.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.MainApplication
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.preference.utils.*
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class PreferenceProvider {
    private var preferences: SharedPreferences = MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    @SuppressLint("CommitPrefEdits")
    fun editRootAccessPreference(choice: Boolean) {
        logd("editRootAccessPreference")
        preferencesEditor = preferences.edit()
        preferencesEditor.putBoolean(PREFERENCE_ROOT_ACCESS, choice)
        preferencesEditor.apply()
    }

    fun getRootAccessPreference(): Boolean? {
        logd("getRootAccessPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(PREFERENCE_ROOT_ACCESS, ROOT_ACCESS_PREF_DEF_VAL)
    }

    @SuppressLint("CommitPrefEdits")
    fun editFileSortPreference(choice: String) {
        logd("editFileSortPreference")
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(PREFERENCE_FILE_SORT, choice)
        preferencesEditor.apply()
    }

    fun getFileSortPreference(): String? {
        logd("getFileSortPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(PREFERENCE_FILE_SORT, DEFAULT_FILE_SORT_MODE)
    }

    @SuppressLint("CommitPrefEdits")
    fun editShowHiddenPreference(choice: Boolean) {
        logd("editShowHiddenPreference")
        preferencesEditor = preferences.edit()
        preferencesEditor.putBoolean(PREFERENCE_SHOW_HIDDEN, choice)
        preferencesEditor.apply()
    }

    fun getShowHiddenPreference(): Boolean {
        logd("getShowHiddenPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(PREFERENCE_SHOW_HIDDEN, SHOW_HIDDEN_PREF_DEF_VAL)
    }

    @SuppressLint("CommitPrefEdits")
    fun editAscendingOrderPreference(choice: Boolean) {
        logd("editAscendingOrderPreference")
        preferencesEditor = preferences.edit()
        preferencesEditor.putBoolean(PREFERENCE_DESCENDING_ORDER, false)
        preferencesEditor.putBoolean(PREFERENCE_ASCENDING_ORDER, choice)
        preferencesEditor.apply()
    }

    fun getAscendingOrderPreference(): Boolean {
        logd("getAscendingOrderPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(PREFERENCE_ASCENDING_ORDER, ASCENDING_ORDER_PREF_DEF_VAL)
    }

    @SuppressLint("CommitPrefEdits")
    fun editDescendingOrderPreference(choice: Boolean) {
        logd("editDescendingOrderPreference")
        preferencesEditor = preferences.edit()
        preferencesEditor.putBoolean(PREFERENCE_ASCENDING_ORDER, false)
        preferencesEditor.putBoolean(PREFERENCE_DESCENDING_ORDER, choice)
        preferencesEditor.apply()
    }

    fun getDescendingOrderPreference(): Boolean {
        logd("getAscendingOrderPreference")
        return MainApplication.appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(PREFERENCE_DESCENDING_ORDER, DESSCENDING_ORDER_PREF_DEF_VAL)
    }
}