package com.erman.usurf.ftp.service

private const val PASSWORD_MASK: String = "***"

data class FtpServerConfig(
    val address: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val homeDirectory: String?,
    val allowUploads: Boolean
) {
    // The password never reaches a log or a crash report through this text.
    override fun toString(): String = "FtpServerConfig(address=$address, port=$port, username=$username, password=$PASSWORD_MASK, " +
        "homeDirectory=$homeDirectory, allowUploads=$allowUploads)"
}

/** One FTP server: started at most once, then stopped. */
interface FtpServerEngine {
    /**
     * Binds the listener and starts serving.
     *
     * @return the port the server listens on
     * @throws FtpServerStartException when the server cannot start
     */
    fun start(): Int

    /** Closes every client session, its data connection first, so no transfer outlives [stop]. */
    fun closeSessions()

    /** Unbinds the listener and releases the server's threads. */
    fun stop()
}

class FtpServerStartException(cause: Throwable) : Exception(cause.message, cause)
