package com.erman.usurf.ftp.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.ftp.utils.DEFAULT_USER_NAME
import com.erman.usurf.ftp.utils.KEY_INTENT_CHOSEN_PATH
import com.erman.usurf.ftp.utils.PASSWORD_DEF_VAL
import com.erman.usurf.ftp.utils.PASSWORD_KEY
import com.erman.usurf.ftp.utils.PORT_KEY
import com.erman.usurf.ftp.utils.USERNAME_KEY
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.logd

class FtpPreferenceProvider(private val preferences: SharedPreferences) {
    fun getUsername(): String? {
        logd("getUsername")
        return preferences.getString(USERNAME_KEY, DEFAULT_USER_NAME)
    }

    fun editUsername(username: String) {
        logd("editUsername")
        preferences.edit { putString(USERNAME_KEY, username) }
    }

    fun getPassword(): String? {
        logd("getPassword")
        return preferences.getString(PASSWORD_KEY, PASSWORD_DEF_VAL)
    }

    fun editPassword(password: String) {
        logd("editPassword")
        preferences.edit { putString(PASSWORD_KEY, password) }
    }

    fun getPort(): Int {
        logd("getPort")
        return preferences.getInt(PORT_KEY, DEFAULT_PORT)
    }

    fun editPort(port: Int) {
        logd("editPort")
        preferences.edit { putInt(PORT_KEY, port) }
    }

    fun getFtpPath(): String? {
        logd("getFtpPath")
        return preferences.getString(KEY_INTENT_CHOSEN_PATH, StoragePaths.getStorageDirectories().first())
    }

    fun editFtpPath(path: String?) {
        logd("editPort")
        preferences.edit { putString(KEY_INTENT_CHOSEN_PATH, path) }
    }
}
