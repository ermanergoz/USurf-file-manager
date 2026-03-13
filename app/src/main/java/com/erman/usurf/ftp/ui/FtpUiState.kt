package com.erman.usurf.ftp.ui

data class FtpUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val storagePaths: List<String> = emptyList(),
    val isConnectedToWifi: Boolean = false,
    val isServiceRunning: Boolean = false,
)

sealed class FtpUiEvent {
    data class ShowSnackbar(val messageResId: Int) : FtpUiEvent()
}
