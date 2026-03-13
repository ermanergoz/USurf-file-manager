package com.erman.usurf.ftp.data

import androidx.lifecycle.LiveData
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FTPLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.service.FtpServer
import com.erman.usurf.storage.domain.StoragePathsProvider

class FtpRepositoryImpl(
    private val ftpModel: FtpModel,
    private val ftpPreferenceProvider: FtpPreferenceProvider,
    private val connectionLiveData: ConnectionLiveData,
    private val ftpLiveData: FTPLiveData,
    private val storagePathsProvider: StoragePathsProvider,
) : FtpRepository {
    override fun getIpAddress(): String = ftpModel.getIpAddress()

    override fun getUsername(): String? = ftpPreferenceProvider.getUsername()

    override fun getPassword(): String? = ftpPreferenceProvider.getPassword()

    override fun getPort(): Int = ftpPreferenceProvider.getPort()

    override fun getStoragePaths(): List<String> = storagePathsProvider.getStorageDirectories().toList()

    override fun getStorageDisplayName(path: String): String = ftpModel.getStorageDisplayName(path)

    override fun isExternalStorage(path: String): Boolean = ftpModel.isExternalStorage(path)

    override fun getFtpPath(): String? = ftpPreferenceProvider.getFtpPath()

    override fun getConnectionLiveData(): LiveData<Boolean> = connectionLiveData

    override fun getFtpServerRunningLiveData(): LiveData<Boolean> = ftpLiveData

    override fun editUsername(username: String) {
        ftpPreferenceProvider.editUsername(username)
    }

    override fun editPassword(password: String) {
        ftpPreferenceProvider.editPassword(password)
    }

    override fun editPort(port: Int) {
        ftpPreferenceProvider.editPort(port)
    }

    override fun editFtpPath(path: String?) {
        ftpPreferenceProvider.editFtpPath(path)
    }

    override fun startServer() {
        ftpModel.startFTPServer()
    }

    override fun stopServer() {
        ftpModel.stopFTPServer()
    }

    override fun isServerRunning(): Boolean = FtpServer.isFtpServerRunning
}
