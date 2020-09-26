package com.erman.usurf.ftp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd

class FtpPreferenceProvider {
    private var preferences: SharedPreferences = appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    fun getUsername(): String? {
        logd("getUsername")
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(USERNAME_KEY, DEFAULT_USER_NAME)
    }

    @SuppressLint("CommitPrefEdits")
    fun editUsername(username: String) {
        logd("editUsername")
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(USERNAME_KEY, username)
        preferencesEditor.apply()
    }

    fun getPassword(): String? {
        logd("getPassword")
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(PASSWORD_KEY, PASSWORD_DEF_VAL)
    }

    @SuppressLint("CommitPrefEdits")
    fun editPassword(password: String) {
        logd("editPassword")
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(PASSWORD_KEY, password)
        preferencesEditor.apply()
    }

    fun getPort(): Int {
        logd("getPort")
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(PORT_KEY, 2221)
    }

    @SuppressLint("CommitPrefEdits")
    fun editPort(port: Int) {
        logd("editPort")
        preferencesEditor = preferences.edit()
        preferencesEditor.putInt(PORT_KEY, port)
        preferencesEditor.apply()
    }
}