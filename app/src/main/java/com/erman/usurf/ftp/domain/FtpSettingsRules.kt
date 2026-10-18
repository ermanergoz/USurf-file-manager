package com.erman.usurf.ftp.domain

const val MIN_FTP_PORT: Int = 1
const val MAX_FTP_PORT: Int = 65535

enum class FtpSettingError {
    USERNAME_REQUIRED,

    // The server never accepts this name: Apache FtpServer reserves it for anonymous login, which stays off.
    USERNAME_ANONYMOUS,
    PASSWORD_REQUIRED,

    // The server drops a space from the end of the password a client sends, so such a password could never log in.
    // One at the start is refused alike.
    PASSWORD_SPACE_AT_ENDS
}

/**
 * The settings a server may start with: a named user, a password that is not blank and has no space at either end, and
 * a port from 1 to 65535.
 */
object FtpSettingsRules {
    fun usernameError(username: String): FtpSettingError? = when {
        username.isBlank() -> FtpSettingError.USERNAME_REQUIRED
        username == ANONYMOUS_FTP_USERNAME -> FtpSettingError.USERNAME_ANONYMOUS
        else -> null
    }

    /**
     * Refuses a blank password and one that starts or ends with a space. A space here is any character up to U+0020 (a
     * space, a tab or a control character): Apache FtpServer trims these from the end of the line a client sends. It
     * keeps them at the start, but they are refused there too, so the rule and its message read the same at both ends.
     * The password is refused, never trimmed.
     */
    fun passwordError(password: String): FtpSettingError? = when {
        password.isBlank() -> FtpSettingError.PASSWORD_REQUIRED
        password.trim { it <= ' ' } != password -> FtpSettingError.PASSWORD_SPACE_AT_ENDS
        else -> null
    }

    /** The port the text names, or null when it is empty, not a number or outside 1 to 65535. */
    fun parsePort(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in MIN_FTP_PORT..MAX_FTP_PORT }
}
