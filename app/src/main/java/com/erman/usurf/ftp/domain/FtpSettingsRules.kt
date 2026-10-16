package com.erman.usurf.ftp.domain

const val MIN_FTP_PORT: Int = 1
const val MAX_FTP_PORT: Int = 65535

enum class FtpSettingError {
    USERNAME_REQUIRED,

    // The server never accepts this name: Apache FtpServer reserves it for anonymous login, which stays off.
    USERNAME_ANONYMOUS,
    PASSWORD_REQUIRED
}

/** The settings a server may start with: a named user, a password that is not blank and a port from 1 to 65535. */
object FtpSettingsRules {
    fun usernameError(username: String): FtpSettingError? = when {
        username.isBlank() -> FtpSettingError.USERNAME_REQUIRED
        username == ANONYMOUS_FTP_USERNAME -> FtpSettingError.USERNAME_ANONYMOUS
        else -> null
    }

    fun passwordError(password: String): FtpSettingError? = if (password.isBlank()) FtpSettingError.PASSWORD_REQUIRED else null

    /** The port the text names, or null when it is empty, not a number or outside 1 to 65535. */
    fun parsePort(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in MIN_FTP_PORT..MAX_FTP_PORT }
}
