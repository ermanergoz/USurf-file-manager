package com.erman.usurf.ftp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erman.usurf.R
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FTPViewModel(private val ftpRepository: FtpRepository) : ViewModel() {
    private val _uiState =
        MediatorLiveData<FtpUiState>().apply {
            value =
                FtpUiState(
                    username = ftpRepository.getUsername().orEmpty(),
                    password = ftpRepository.getPassword().orEmpty(),
                    port = ftpRepository.getPort().toString(),
                    isConnectedToWifi = ftpRepository.getConnectionLiveData().value == true,
                    isServiceRunning = ftpRepository.getFtpServerRunningLiveData().value == true
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

    init {
        loadServerDetails()
    }

    private fun loadServerDetails() {
        viewModelScope.launch {
            val url = withContext(Dispatchers.IO) { ftpRepository.getIpAddress() }
            val storagePaths = withContext(Dispatchers.IO) { mapStoragePaths(ftpRepository.getStoragePaths()) }
            updateState { it.copy(url = url, storagePaths = storagePaths) }
        }
    }

    private fun updateState(transform: (FtpUiState) -> FtpUiState) {
        _uiState.value = transform(_uiState.value ?: FtpUiState())
    }

    private fun mapStoragePaths(paths: List<String>): List<StoragePathItem> = paths.map { path ->
        StoragePathItem(
            path = path,
            displayName = ftpRepository.getStorageDisplayName(path),
            isExternal = ftpRepository.isExternalStorage(path)
        )
    }

    fun onConnectClicked() {
        if (!ftpRepository.isServerRunning()) {
            ftpRepository.startServer()
        } else {
            ftpRepository.stopServer()
        }
    }

    fun onCopyUrlClicked() {
        val url: String = _uiState.value?.url.orEmpty()
        if (url.isNotEmpty()) {
            _uiEvents.value = Event(FtpUiEvent.CopyToClipboard(url))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onUsernameChanged(username: CharSequence, start: Int, before: Int, count: Int) {
        val str = username.toString()
        ftpRepository.editUsername(str)
        updateState { it.copy(username = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPasswordChanged(password: CharSequence, start: Int, before: Int, count: Int) {
        val str = password.toString()
        ftpRepository.editPassword(str)
        updateState { it.copy(password = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPortChanged(port: CharSequence, start: Int, before: Int, count: Int) {
        val newPort: Int =
            try {
                port.toString().toInt()
            } catch (err: NumberFormatException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
                _uiEvents.value = Event(FtpUiEvent.ShowSnackbar(R.string.port_error))
                DEFAULT_PORT
            }
        ftpRepository.editPort(newPort)
        updateState { it.copy(port = port.toString()) }
    }

    fun onFtpPathSelected(checkedId: Int) {
        val paths: List<StoragePathItem> = _uiState.value?.storagePaths ?: emptyList()
        ftpRepository.editFtpPath(paths.elementAtOrNull(checkedId)?.path)
    }

    fun getFtpSelectedPath(): String? = ftpRepository.getFtpPath()
}
