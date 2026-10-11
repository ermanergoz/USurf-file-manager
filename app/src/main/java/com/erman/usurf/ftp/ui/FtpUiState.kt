package com.erman.usurf.ftp.ui

import androidx.annotation.StringRes
import com.erman.usurf.R
import com.erman.usurf.ftp.domain.FtpServerStatus

data class StoragePathItem(val path: String, val displayName: String, val isExternal: Boolean)

data class FtpUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val storagePaths: List<StoragePathItem> = emptyList(),
    val isConnectedToWifi: Boolean = false,
    val serverStatus: FtpServerStatus = FtpServerStatus.Stopped
) {
    val isServerRunning: Boolean
        get() = serverStatus is FtpServerStatus.Running

    /** Starting, running or stopping: the settings cannot change. A failed start is not active. */
    val isServerActive: Boolean
        get() = serverStatus !is FtpServerStatus.Stopped && serverStatus !is FtpServerStatus.Failed

    val isConnectButtonEnabled: Boolean
        get() = when (serverStatus) {
            is FtpServerStatus.Running -> true
            FtpServerStatus.Starting, FtpServerStatus.Stopping -> false
            FtpServerStatus.Stopped, is FtpServerStatus.Failed -> isConnectedToWifi
        }

    @get:StringRes
    val connectButtonTextRes: Int
        get() = when (serverStatus) {
            is FtpServerStatus.Running, FtpServerStatus.Stopping -> R.string.disconnect
            FtpServerStatus.Stopped, FtpServerStatus.Starting, is FtpServerStatus.Failed -> R.string.connect
        }

    @get:StringRes
    val statusTextRes: Int
        get() = when (serverStatus) {
            is FtpServerStatus.Running -> R.string.ftp_server_active
            FtpServerStatus.Starting -> R.string.ftp_server_starting
            FtpServerStatus.Stopping -> R.string.ftp_server_stopping
            FtpServerStatus.Stopped, is FtpServerStatus.Failed ->
                if (isConnectedToWifi) R.string.connected else R.string.no_connection
        }

    /** The address a client connects to: with the port while the server runs. */
    val displayedUrl: String
        get() = (serverStatus as? FtpServerStatus.Running)?.url ?: url
}

sealed class FtpUiEvent {
    data class ShowSnackbar(val messageResId: Int) : FtpUiEvent()
    data class CopyToClipboard(val url: String) : FtpUiEvent()
    data class ShowStartFailure(val reason: String) : FtpUiEvent()
}
