package com.erman.usurf.ftp.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import com.erman.usurf.R
import com.erman.usurf.ftp.domain.FtpFailureReason
import com.erman.usurf.ftp.domain.FtpServerStatus
import com.erman.usurf.ftp.domain.FtpSettingError
import com.erman.usurf.ftp.domain.FtpSettingsRules
import com.erman.usurf.storage.domain.StorageKind

/** A string id of 0: the field has no error. */
const val NO_ERROR: Int = 0

/** One storage directory the server can share, with its kind, which the screen names it by. */
data class StoragePathItem(val path: String, val kind: StorageKind) {
    val isExternal: Boolean
        get() = kind == StorageKind.SD_CARD
}

data class FtpUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val allowUploads: Boolean = false,
    val storagePaths: List<StoragePathItem> = emptyList(),
    /** The storage directory the server shares, as stored, then as chosen on the screen; none when there is none. */
    val selectedPath: String? = null,
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

    /** Whether an address was read for this device; without one the idle screen says it is not available yet. */
    val isAddressAvailable: Boolean
        get() = displayedUrl.isNotEmpty()

    /** The address a client connects to: with the port while the server runs. */
    val displayedUrl: String
        get() = (serverStatus as? FtpServerStatus.Running)?.url ?: url

    /** The address can be copied while the server runs and the device is on Wi-Fi. */
    val isCopyUrlVisible: Boolean
        get() = isConnectedToWifi && isServerRunning

    /** The address line: without Wi-Fi, how to get an address; without one read, that there is none yet; else the address. */
    fun urlText(resources: Resources): String = when {
        !isConnectedToWifi -> resources.getString(R.string.ftp_not_connected_hint)
        !isAddressAvailable -> resources.getString(R.string.ftp_address_unavailable)
        else -> displayedUrl
    }
}

@get:StringRes
private val FtpSettingError.messageRes: Int
    get() = when (this) {
        FtpSettingError.USERNAME_REQUIRED -> R.string.ftp_username_required
        FtpSettingError.USERNAME_ANONYMOUS -> R.string.ftp_username_anonymous
        FtpSettingError.USERNAME_SPACE_AT_ENDS -> R.string.ftp_username_space_at_ends
        FtpSettingError.USERNAME_CONTROL_CHARACTER -> R.string.ftp_username_control_character
        FtpSettingError.PASSWORD_REQUIRED -> R.string.ftp_password_required
        FtpSettingError.PASSWORD_SPACE_AT_ENDS -> R.string.ftp_password_space_at_ends
        FtpSettingError.PASSWORD_CONTROL_CHARACTER -> R.string.ftp_password_control_character
    }

/** The reason in the language of [resources]: it follows "Could not start the server: ", so it starts lower case. */
fun FtpFailureReason.text(resources: Resources): String = when (this) {
    FtpFailureReason.SettingsInvalid -> resources.getString(R.string.ftp_reason_settings_invalid)
    is FtpFailureReason.PortInUse -> resources.getString(R.string.ftp_reason_port_in_use, port)
    is FtpFailureReason.PortRestricted -> resources.getString(R.string.ftp_reason_port_restricted)
    is FtpFailureReason.Other -> message
}

sealed class FtpUiEvent {
    data class CopyToClipboard(val url: String) : FtpUiEvent()

    data class CopyPassword(val password: String) : FtpUiEvent()

    data class ShowStartFailure(val reason: FtpFailureReason) : FtpUiEvent()

    data class ShowStopFailure(val reason: FtpFailureReason) : FtpUiEvent()
}
