package com.erman.usurf.ftp.domain

import androidx.lifecycle.LiveData
import com.erman.usurf.storage.domain.StorageKind

/** The FTP server and what it serves on: its status, the Wi-Fi connection, the device's address and the storage. */
interface FtpRepository {
    /** @throws java.net.SocketException when the device's network interfaces cannot be read. */
    fun getIpAddress(): String

    fun getStoragePaths(): List<String>

    /** What the storage directory [path] is, which the screen names it by. */
    fun storageKindOf(path: String): StorageKind

    fun getConnectionLiveData(): LiveData<Boolean>

    fun getServerStatus(): LiveData<FtpServerStatus>

    fun startServer()

    fun stopServer()
}
