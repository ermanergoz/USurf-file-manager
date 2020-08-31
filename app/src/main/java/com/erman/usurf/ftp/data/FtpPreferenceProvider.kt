package com.erman.usurf.ftp.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.utils.SHARED_PREF_FILE

class FtpPreferenceProvider {
    private var preferences: SharedPreferences = appContext.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
    lateinit var preferencesEditor: SharedPreferences.Editor

    fun getUsername(): String? {
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(USERNAME_KEY, DEFAULT_USER_NAME)
    }

    @SuppressLint("CommitPrefEdits")
    fun editUsername(username: String) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(USERNAME_KEY, username)
        preferencesEditor.apply()
    }

    fun getPassword(): String? {
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(PASSWORD_KEY, PASSWORD_DEF_VAL)
    }

    @SuppressLint("CommitPrefEdits")
    fun editPassword(password: String) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(PASSWORD_KEY, password)
        preferencesEditor.apply()
    }

    fun getPort(): Int {
        return appContext.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(PORT_KEY, 2221)
    }

    @SuppressLint("CommitPrefEdits")
    fun editPort(port: Int) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putInt(PORT_KEY, port)
        preferencesEditor.apply()
    }
}