package com.erman.usurf.ftp.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.ftp.utils.PORT_KEY
import com.erman.usurf.storage.domain.StoragePathsProvider

private const val PASSWORD_KEY: String = "password"
private const val USERNAME_KEY: String = "username"
private const val PASSWORD_DEF_VAL: String = ""
private const val KEY_INTENT_CHOSEN_PATH: String = "ftpChosenPath"
private const val DEFAULT_USER_NAME: String = "anonymous"

class FtpPreferenceProvider(
    private val preferences: SharedPreferences,
    private val storagePathsProvider: StoragePathsProvider,
) {
    fun getUsername(): String? {
        return preferences.getString(USERNAME_KEY, DEFAULT_USER_NAME)
    }

    fun editUsername(username: String) {
        preferences.edit { putString(USERNAME_KEY, username) }
    }

    fun getPassword(): String? {
        return preferences.getString(PASSWORD_KEY, PASSWORD_DEF_VAL)
    }

    fun editPassword(password: String) {
        preferences.edit { putString(PASSWORD_KEY, password) }
    }

    fun getPort(): Int {
        return preferences.getInt(PORT_KEY, DEFAULT_PORT)
    }

    fun editPort(port: Int) {
        preferences.edit { putInt(PORT_KEY, port) }
    }

    fun getFtpPath(): String? {
        val defaultPath: String? = storagePathsProvider.getStorageDirectories().firstOrNull()
        return preferences.getString(KEY_INTENT_CHOSEN_PATH, defaultPath)
    }

    fun editFtpPath(path: String?) {
        preferences.edit { putString(KEY_INTENT_CHOSEN_PATH, path) }
    }
}
