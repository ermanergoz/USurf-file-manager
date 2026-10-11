package com.erman.usurf.ftp.ui

import androidx.annotation.StringRes
import com.erman.usurf.R
import com.erman.usurf.ftp.domain.FtpServerStatus
import com.erman.usurf.ftp.domain.FtpSettingError
import com.erman.usurf.ftp.domain.FtpSettingsRules

/** A string id of 0: the field has no error. */
const val NO_ERROR: Int = 0

data class StoragePathItem(val path: String, val displayName: String, val isExternal: Boolean)

data class FtpUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val allowUploads: Boolean = false,
    val storagePaths: List<StoragePathItem> = emptyList(),
    val isConnectedToWifi: Boolean = false,
    val serverStatus: FtpServerStatus = FtpServerStatus.Stopped
) {
    val isServerRunning: Boolean
        get() = serverStatus is FtpServerStatus.Running

    /** Starting, running or stopping: the settings cannot change. A failed start is not active. */
    val isServerActive: Boolean
        get() = serverStatus !is FtpServerStatus.Stopped && serverStatus !is FtpServerStatus.Failed

    @get:StringRes
    val usernameErrorRes: Int
        get() = FtpSettingsRules.usernameError(username)?.messageRes ?: NO_ERROR

    @get:StringRes
    val passwordErrorRes: Int
        get() = FtpSettingsRules.passwordError(password)?.messageRes ?: NO_ERROR

    @get:StringRes
    val portErrorRes: Int
        get() = if (FtpSettingsRules.parsePort(port) == null) R.string.ftp_port_out_of_range else NO_ERROR

    /** The server may start with what the fields hold: they hold what is stored. */
    val hasValidSettings: Boolean
        get() = usernameErrorRes == NO_ERROR && passwordErrorRes == NO_ERROR && portErrorRes == NO_ERROR

    val isConnectButtonEnabled: Boolean
        get() = when (serverStatus) {
            is FtpServerStatus.Running -> true
            FtpServerStatus.Starting, FtpServerStatus.Stopping -> false
            FtpServerStatus.Stopped, is FtpServerStatus.Failed -> isConnectedToWifi && hasValidSettings
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

@get:StringRes
private val FtpSettingError.messageRes: Int
    get() = when (this) {
        FtpSettingError.USERNAME_REQUIRED -> R.string.ftp_username_required
        FtpSettingError.USERNAME_ANONYMOUS -> R.string.ftp_username_anonymous
        FtpSettingError.PASSWORD_REQUIRED -> R.string.ftp_password_required
    }

sealed class FtpUiEvent {
    data class CopyToClipboard(val url: String) : FtpUiEvent()

    data class CopyPassword(val password: String) : FtpUiEvent()
    data class ShowStartFailure(val reason: String) : FtpUiEvent()
}
