package com.erman.usurf.ftp.service

data class FtpServerConfig(
    val address: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val homeDirectory: String?,
    val allowUploads: Boolean
)

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
