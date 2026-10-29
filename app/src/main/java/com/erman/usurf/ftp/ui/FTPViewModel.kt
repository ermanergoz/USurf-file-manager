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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FTPViewModel(
    private val ftpRepository: FtpRepository,
    private val ftpSettingsRepository: FtpSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var lastServerStatus: FtpServerStatus? = null

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
                    isConnectedToWifi = ftpRepository.getConnectionLiveData().value == true
                )
            addSource(ftpRepository.getConnectionLiveData()) { connected ->
                value = (value ?: FtpUiState()).copy(isConnectedToWifi = connected == true)
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
        viewModelScope.launch {
            val url: String = withContext(ioDispatcher) { readIpAddress() } ?: return@launch
            updateState { it.copy(url = url) }
        }
        viewModelScope.launch {
            val storagePaths = withContext(ioDispatcher) { mapStoragePaths(ftpRepository.getStoragePaths()) }
            updateState { it.copy(storagePaths = storagePaths) }
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
            FtpServerStatus.Stopped, is FtpServerStatus.Failed -> if (state.hasValidSettings) ftpRepository.startServer()
            is FtpServerStatus.Running -> ftpRepository.stopServer()
            FtpServerStatus.Starting, FtpServerStatus.Stopping -> Unit
        }
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

    // The fields keep what is typed; only a valid value is stored, so the server never starts on settings the screen
    // marks as wrong.
    @Suppress("UNUSED_PARAMETER")
    fun onUsernameChanged(username: CharSequence, start: Int, before: Int, count: Int) {
        val str = username.toString()
        if (FtpSettingsRules.usernameError(str) == null) ftpSettingsRepository.editUsername(str)
        updateState { it.copy(username = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPasswordChanged(password: CharSequence, start: Int, before: Int, count: Int) {
        val str = password.toString()
        if (FtpSettingsRules.passwordError(str) == null) ftpSettingsRepository.editPassword(str)
        updateState { it.copy(password = str) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPortChanged(port: CharSequence, start: Int, before: Int, count: Int) {
        val str = port.toString()
        FtpSettingsRules.parsePort(str)?.let(ftpSettingsRepository::editPort)
        updateState { it.copy(port = str) }
    }

    // Read when the server starts: the switch is disabled while it runs.
    fun onAllowUploadsChanged(allowed: Boolean) {
        ftpSettingsRepository.editUploadAllowed(allowed)
        updateState { it.copy(allowUploads = allowed) }
    }

    fun onFtpPathSelected(path: String) {
        ftpSettingsRepository.editFtpPath(path)
    }

    fun getFtpSelectedPath(): String? = ftpSettingsRepository.getFtpPath()
}
