package com.erman.usurf.ftp.domain

/** Why a start or a stop of the FTP server did not succeed, in a form the screen puts into the user's language. */
sealed interface FtpFailureReason {
    /** The stored login is one the server refuses, as an anonymous name or a missing password. */
    data object SettingsInvalid : FtpFailureReason

    /** Another server listens on [port] already. */
    data class PortInUse(val port: Int) : FtpFailureReason

    /** The device lets no app listen on [port], one below 1024. */
    data class PortRestricted(val port: Int) : FtpFailureReason

    /** Anything else, with the server library's own [message]. */
    data class Other(val message: String) : FtpFailureReason
}
