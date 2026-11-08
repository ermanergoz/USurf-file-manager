package com.erman.usurf.ftp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.domain.FtpServerStatus
import com.erman.usurf.ftp.domain.FtpSettingsRepository
import com.erman.usurf.ftp.domain.FtpSettingsRules
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.net.SocketException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FTPViewModel(
    private val ftpRepository: FtpRepository,
    private val ftpSettingsRepository: FtpSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var lastServerStatus: FtpServerStatus? = null

    // The read of the device's address in flight: a newer read replaces it, so an older answer never wins.
    private var addressRead: Job? = null

    private val _uiEvents = MutableLiveData<Event<FtpUiEvent>>()
    val uiEvents: LiveData<Event<FtpUiEvent>> = _uiEvents

    private val _uiState =
        MediatorLiveData<FtpUiState>().apply {
            value =
                FtpUiState(
                    username = ftpSettingsRepository.getUsername(),
                    password = ftpSettingsRepository.getPassword(),
                    port = ftpSettingsRepository.getPort().toString(),
                    allowUploads = ftpSettingsRepository.isUploadAllowed(),
                    selectedPath = ftpSettingsRepository.getFtpPath(),
                    isConnectedToWifi = ftpRepository.getConnectionLiveData().value == true
                )
            // The address read on opening is none while off Wi-Fi; it is read again each time Wi-Fi connects.
            addSource(ftpRepository.getConnectionLiveData()) { connected ->
                val wasConnected: Boolean = value?.isConnectedToWifi == true
                value = (value ?: FtpUiState()).copy(isConnectedToWifi = connected == true)
                if (connected == true && !wasConnected) loadAddress()
            }
            addSource(ftpRepository.getServerStatus()) { status ->
                onServerStatusChanged(status)
            }
        }
    val uiState: LiveData<FtpUiState> = _uiState

    /** The storage paths the radio group lists: emitted only when they change, never on a keystroke in a field. */
    val storagePaths: LiveData<List<StoragePathItem>> = _uiState.map { it.storagePaths }.distinctUntilChanged()

    init {
        loadServerDetails()
    }

    // The address and the storage paths load apart: a device whose address cannot be read still lists its paths.
    private fun loadServerDetails() {
        loadAddress()
        viewModelScope.launch {
            val storagePaths = withContext(ioDispatcher) { mapStoragePaths(ftpRepository.getStoragePaths()) }
            updateState { it.copy(storagePaths = storagePaths) }
        }
    }

    private fun loadAddress() {
        addressRead?.cancel()
        addressRead =
            viewModelScope.launch {
                val url: String = withContext(ioDispatcher) { readIpAddress() } ?: return@launch
                updateState { it.copy(url = url) }
            }
    }

    // An unreadable address leaves the screen without one; a start on such a device fails with its own reason.
    private fun readIpAddress(): String? = try {
        ftpRepository.getIpAddress()
    } catch (err: SocketException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        null
    }

    // A failure is shown once, when this screen sees a start or a stop fail; an old failure read on opening is not.
    private fun onServerStatusChanged(status: FtpServerStatus) {
        val previous: FtpServerStatus? = lastServerStatus
        lastServerStatus = status
        if (status is FtpServerStatus.Failed && previous != null && previous !== status) {
            val failure = if (previous is FtpServerStatus.Stopping) {
                FtpUiEvent.ShowStopFailure(
                    status.reason
                )
            } else {
                FtpUiEvent.ShowStartFailure(status.reason)
            }
            _uiEvents.value = Event(failure)
        }
        updateState { it.copy(serverStatus = status) }
    }

    private fun updateState(transform: (FtpUiState) -> FtpUiState) {
        _uiState.value = transform(_uiState.value ?: FtpUiState())
    }

    private fun mapStoragePaths(paths: List<String>): List<StoragePathItem> =
        paths.map { path -> StoragePathItem(path = path, kind = ftpRepository.storageKindOf(path)) }

    fun onConnectClicked() {
        val state: FtpUiState = _uiState.value ?: return
        when (state.serverStatus) {
            FtpServerStatus.Stopped, is FtpServerStatus.Failed -> if (state.hasValidSettings) start(state)
            is FtpServerStatus.Running -> ftpRepository.stopServer()
            FtpServerStatus.Starting, FtpServerStatus.Stopping -> Unit
        }
    }

    // The typed login and port are stored here, once, and only when the screen accepts all of them, so the server never
    // starts on settings the screen marks as wrong and a half-typed value is never kept.
    private fun start(state: FtpUiState) {
        ftpSettingsRepository.editUsername(state.username)
        ftpSettingsRepository.editPassword(state.password)
        FtpSettingsRules.parsePort(state.port)?.let(ftpSettingsRepository::editPort)
        ftpRepository.startServer()
    }

    fun onCopyUrlClicked() {
        val url: String = _uiState.value?.displayedUrl.orEmpty()
        if (url.isNotEmpty()) {
            _uiEvents.value = Event(FtpUiEvent.CopyToClipboard(url))
        }
    }

    fun onCopyPasswordClicked() {
        val password: String = _uiState.value?.password.orEmpty()
        if (password.isNotEmpty()) {
            _uiEvents.value = Event(FtpUiEvent.CopyPassword(password))
        }
    }

    // The fields keep what is typed, and nothing is stored before Start. A field also reports the text the state gave it,
    // each time the binding sets it; that is no change and is let go.
    fun onUsernameChanged(username: String) {
        if (username != _uiState.value?.username) updateState { it.copy(username = username) }
    }

    fun onPasswordChanged(password: String) {
        if (password != _uiState.value?.password) updateState { it.copy(password = password) }
    }

    fun onPortChanged(port: String) {
        if (port != _uiState.value?.port) updateState { it.copy(port = port) }
    }

    // Read when the server starts: the switch is disabled while it runs.
    fun onAllowUploadsChanged(allowed: Boolean) {
        ftpSettingsRepository.editUploadAllowed(allowed)
        updateState { it.copy(allowUploads = allowed) }
    }

    // The group also reports the button the state checked, each time the binding checks it; that is no new choice.
    fun onFtpPathSelected(path: String) {
        if (path == _uiState.value?.selectedPath) return
        ftpSettingsRepository.editFtpPath(path)
        updateState { it.copy(selectedPath = path) }
    }
}
