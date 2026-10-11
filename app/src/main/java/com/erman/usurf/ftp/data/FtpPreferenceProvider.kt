package com.erman.usurf.ftp.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.ftp.domain.FtpCredentials
import com.erman.usurf.ftp.domain.FtpCredentialsInitializer
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.ftp.utils.PORT_KEY
import com.erman.usurf.storage.domain.StoragePathsProvider

private const val PASSWORD_KEY: String = "password"
private const val USERNAME_KEY: String = "username"
private const val CREDENTIALS_INITIALIZED_KEY: String = "ftpCredentialsInitialized"
private const val KEY_INTENT_CHOSEN_PATH: String = "ftpChosenPath"
private const val ALLOW_UPLOADS_KEY: String = "ftpAllowUploads"

class FtpPreferenceProvider(
    private val preferences: SharedPreferences,
    private val storagePathsProvider: StoragePathsProvider,
    private val credentialsInitializer: FtpCredentialsInitializer
) {
    fun getUsername(): String {
        initializeCredentials()
        return preferences.getString(USERNAME_KEY, null).orEmpty()
    }

    fun editUsername(username: String) {
        preferences.edit { putString(USERNAME_KEY, username) }
    }

    fun getPassword(): String {
        initializeCredentials()
        return preferences.getString(PASSWORD_KEY, null).orEmpty()
    }

    fun editPassword(password: String) {
        preferences.edit { putString(PASSWORD_KEY, password) }
    }

    // Runs once per install, on the first read: a new install, or an update over an open anonymous login.
    @Synchronized
    private fun initializeCredentials() {
        if (preferences.getBoolean(CREDENTIALS_INITIALIZED_KEY, false)) return
        val credentials: FtpCredentials =
            credentialsInitializer.initialCredentials(
                storedUsername = preferences.getString(USERNAME_KEY, null),
                storedPassword = preferences.getString(PASSWORD_KEY, null)
            )
        preferences.edit {
            putString(USERNAME_KEY, credentials.username)
            putString(PASSWORD_KEY, credentials.password)
            putBoolean(CREDENTIALS_INITIALIZED_KEY, true)
        }
    }

    fun getPort(): Int = preferences.getInt(PORT_KEY, DEFAULT_PORT)

    fun editPort(port: Int) {
        preferences.edit { putInt(PORT_KEY, port) }
    }

    fun isUploadAllowed(): Boolean = preferences.getBoolean(ALLOW_UPLOADS_KEY, false)

    fun editUploadAllowed(allowed: Boolean) {
        preferences.edit { putBoolean(ALLOW_UPLOADS_KEY, allowed) }
    }

    fun getFtpPath(): String? {
        val defaultPath: String? = storagePathsProvider.getStorageDirectories().firstOrNull()
        return preferences.getString(KEY_INTENT_CHOSEN_PATH, defaultPath)
    }

    fun editFtpPath(path: String?) {
        preferences.edit { putString(KEY_INTENT_CHOSEN_PATH, path) }
    }
}
