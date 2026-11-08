package com.erman.usurf.ftp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.domain.FtpServerStatus
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.service.FtpServiceLauncher
import com.erman.usurf.storage.domain.StorageKind
import com.erman.usurf.storage.domain.StoragePathsProvider

class FtpRepositoryImpl(
    private val ftpModel: FtpModel,
    private val connectionLiveData: ConnectionLiveData,
    private val serverState: FtpServerState,
    private val storagePathsProvider: StoragePathsProvider,
    private val serviceLauncher: FtpServiceLauncher
) : FtpRepository {
    override fun getIpAddress(): String = ftpModel.getIpAddress()

    override fun getStoragePaths(): List<String> = storagePathsProvider.getStorageDirectories().toList()

    override fun storageKindOf(path: String): StorageKind = ftpModel.storageKindOf(path)

    override fun getConnectionLiveData(): LiveData<Boolean> = connectionLiveData

    // A new LiveData per screen: it starts from the state as it is now, never from a value cached while no screen watched.
    override fun getServerStatus(): LiveData<FtpServerStatus> = serverState.status.asLiveData()

    override fun startServer() {
        serviceLauncher.start()
    }

    override fun stopServer() {
        serviceLauncher.stop()
    }
}
