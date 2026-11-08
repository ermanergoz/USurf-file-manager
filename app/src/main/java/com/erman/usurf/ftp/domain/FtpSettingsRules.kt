package com.erman.usurf.ftp.domain

const val MIN_FTP_PORT: Int = 1
const val MAX_FTP_PORT: Int = 65535

enum class FtpSettingError {
    USERNAME_REQUIRED,

    // The server never accepts this name: Apache FtpServer reserves it for anonymous login, which stays off.
    USERNAME_ANONYMOUS,

    // The server drops a space from the end of the name a client sends, so the stored name is never found.
    // One at the start is refused alike.
    USERNAME_SPACE_AT_ENDS,

    // A line feed splits the line a client sends, and no control character can be typed on another device.
    USERNAME_CONTROL_CHARACTER,
    PASSWORD_REQUIRED,

    // The server drops a space from the end of the password a client sends, so such a password could never log in.
    // One at the start is refused alike.
    PASSWORD_SPACE_AT_ENDS,

    // A line feed splits the line a client sends, and no control character can be typed on another device.
    PASSWORD_CONTROL_CHARACTER
}

/**
 * The settings a server may start with: a named user and a password that is not blank, neither of them with a space at
 * either end or a control character inside, and a port from 1 to 65535.
 */
object FtpSettingsRules {
    fun usernameError(username: String): FtpSettingError? = when {
        username.isBlank() -> FtpSettingError.USERNAME_REQUIRED
        username == ANONYMOUS_FTP_USERNAME -> FtpSettingError.USERNAME_ANONYMOUS
        username.trim { it <= ' ' } != username -> FtpSettingError.USERNAME_SPACE_AT_ENDS
        username.any { it < ' ' } -> FtpSettingError.USERNAME_CONTROL_CHARACTER
        else -> null
    }

    /**
     * Refuses a blank password, one that starts or ends with a space and one with a control character inside. A space
     * here is any character up to U+0020 (a space, a tab or a control character): Apache FtpServer trims these from the
     * end of the line a client sends. It keeps them at the start, but they are refused there too, so the rule and its
     * message read the same at both ends. A control character is any below U+0020: a line feed splits the line a client
     * sends, and none can be typed on another device. The ends are checked first, so a tab at the end reads as a space.
     * [usernameError] refuses a name the same way. The password and the name are refused, never trimmed.
     */
    fun passwordError(password: String): FtpSettingError? = when {
        password.isBlank() -> FtpSettingError.PASSWORD_REQUIRED
        password.trim { it <= ' ' } != password -> FtpSettingError.PASSWORD_SPACE_AT_ENDS
        password.any { it < ' ' } -> FtpSettingError.PASSWORD_CONTROL_CHARACTER
        else -> null
    }

    /** The port the text names, or null when it is empty, not a number or outside 1 to 65535. */
    fun parsePort(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in MIN_FTP_PORT..MAX_FTP_PORT }
}
