package com.erman.usurf.ftp.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge

class FTPViewModel(
    private val ftpRepository: FtpRepository,
) : ViewModel() {
    private val _uiState =
        MediatorLiveData<FtpUiState>().apply {
            value =
                FtpUiState(
                    url = ftpRepository.getIpAddress(),
                    username = ftpRepository.getUsername().orEmpty(),
                    password = ftpRepository.getPassword().orEmpty(),
                    port = ftpRepository.getPort().toString(),
                    storagePaths = ftpRepository.getStoragePaths(),
                    isConnectedToWifi = ftpRepository.getConnectionLiveData().value == true,
                    isServiceRunning = ftpRepository.getFtpServerRunningLiveData().value == true,
                )
            addSource(ftpRepository.getConnectionLiveData()) { connected ->
                value = (value ?: FtpUiState()).copy(isConnectedToWifi = connected == true)
            }
            addSource(ftpRepository.getFtpServerRunningLiveData()) { running ->
                value = (value ?: FtpUiState()).copy(isServiceRunning = running == true)
            }
        }
    val uiState: LiveData<FtpUiState> = _uiState

    private val _uiEvents = MutableLiveData<Event<FtpUiEvent>>()
    val uiEvents: LiveData<Event<FtpUiEvent>> = _uiEvents

    private fun updateState(transform: (FtpUiState) -> FtpUiState) {
        _uiState.value = transform(_uiState.value ?: FtpUiState())
    }

    fun onConnectClicked() {
        Log.e("connection stat", ftpRepository.isServerRunning().toString())
        if (!ftpRepository.isServerRunning()) {
            ftpRepository.startServer()
        } else {
            ftpRepository.stopServer()
        }
    }

    fun getServerStatus(): Boolean = ftpRepository.isServerRunning()

    @Suppress("UNUSED_PARAMETER")
    fun onUsernameChanged(
        username: CharSequence,
        start: Int,
        before: Int,
        count: Int,
    ) {
        val str = username.toString()
        ftpRepository.editUsername(str)
        updateState { it.copy(username = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPasswordChanged(
        password: CharSequence,
        start: Int,
        before: Int,
        count: Int,
    ) {
        val str = password.toString()
        ftpRepository.editPassword(str)
        updateState { it.copy(password = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPortChanged(
        port: CharSequence,
        start: Int,
        before: Int,
        count: Int,
    ) {
        var newPort: Int
        try {
            newPort = port.toString().toInt()
        } catch (err: NumberFormatException) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            newPort = DEFAULT_PORT
            _uiEvents.value = Event(FtpUiEvent.ShowSnackbar(R.string.port_error))
        }
        ftpRepository.editPort(newPort)
        updateState { it.copy(port = port.toString()) }
    }

    fun onFtpPathSelected(checkedId: Int) {
        val paths = _uiState.value?.storagePaths ?: emptyList()
        ftpRepository.editFtpPath(paths.elementAtOrNull(checkedId))
    }

    fun getFtpSelectedPath(): String? = ftpRepository.getFtpPath()
}
