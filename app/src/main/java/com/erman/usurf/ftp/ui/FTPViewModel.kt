package com.erman.usurf.ftp.ui

import android.util.Log
import android.widget.RadioGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MediatorLiveData
import com.erman.usurf.R
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FTPLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.model.FtpServer
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.loge

class FTPViewModel(
    private val ftpModel: FtpModel,
    private val preferenceProvider: FtpPreferenceProvider,
    private val connectionLiveData: ConnectionLiveData,
    private val ftpLiveData: FTPLiveData,
) : ViewModel() {

    private val _uiState = MediatorLiveData<FtpUiState>().apply {
        value = FtpUiState(
            url = ftpModel.getIpAddress(),
            username = preferenceProvider.getUsername().orEmpty(),
            password = preferenceProvider.getPassword().orEmpty(),
            port = preferenceProvider.getPort().toString(),
            storagePaths = StoragePaths.getStorageDirectories().toList(),
            isConnectedToWifi = connectionLiveData.value == true,
            isServiceRunning = ftpLiveData.value == true,
        )
        addSource(connectionLiveData) { connected ->
            value = (value ?: FtpUiState()).copy(isConnectedToWifi = connected == true)
        }
        addSource(ftpLiveData) { running ->
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
        Log.e("connection stat", FtpServer.isFtpServerRunning.toString())
        if (!FtpServer.isFtpServerRunning) {
            ftpModel.startFTPServer()
        } else {
            ftpModel.stopFTPServer()
        }
    }

    fun getServerStatus(): Boolean = FtpServer.isFtpServerRunning

    @Suppress("UNUSED_PARAMETER")
    fun onUsernameChanged(
        username: CharSequence,
        start: Int,
        before: Int,
        count: Int,
    ) {
        val str = username.toString()
        preferenceProvider.editUsername(str)
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
        preferenceProvider.editPassword(str)
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
            loge("onPortChanged $err")
            newPort = DEFAULT_PORT
            _uiEvents.value = Event(FtpUiEvent.ShowToast(R.string.port_error))
        }
        preferenceProvider.editPort(newPort)
        updateState { it.copy(port = port.toString()) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFtpPathSelected(
        radioGroup: RadioGroup,
        id: Int,
    ) {
        val paths = _uiState.value?.storagePaths ?: emptyList()
        preferenceProvider.editFtpPath(paths.elementAtOrNull(id))
    }

    fun getFtpSelectedPath(): String? = preferenceProvider.getFtpPath()
}
