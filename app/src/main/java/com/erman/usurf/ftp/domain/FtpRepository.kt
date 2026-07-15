package com.erman.usurf.ftp.domain

import androidx.lifecycle.LiveData

interface FtpRepository {
    fun getIpAddress(): String

    fun getUsername(): String?

    fun getPassword(): String?

    fun getPort(): Int

    fun getStoragePaths(): List<String>

    fun getStorageDisplayName(path: String): String

    fun isExternalStorage(path: String): Boolean

    fun getFtpPath(): String?

    fun getConnectionLiveData(): LiveData<Boolean>

    fun getFtpServerRunningLiveData(): LiveData<Boolean>

    fun editUsername(username: String)

    fun editPassword(password: String)

    fun editPort(port: Int)

    fun editFtpPath(path: String?)

    fun startServer()

    fun stopServer()

    fun isServerRunning(): Boolean
}
