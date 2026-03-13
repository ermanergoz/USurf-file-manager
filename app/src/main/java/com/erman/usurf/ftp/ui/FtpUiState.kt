package com.erman.usurf.ftp.ui

data class StoragePathItem(
    val path: String,
    val displayName: String,
    val isExternal: Boolean,
)

data class FtpUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val storagePaths: List<StoragePathItem> = emptyList(),
    val isConnectedToWifi: Boolean = false,
    val isServiceRunning: Boolean = false,
)

sealed class FtpUiEvent {
    data class ShowSnackbar(val messageResId: Int) : FtpUiEvent()
    data class CopyToClipboard(val url: String) : FtpUiEvent()
}
