package com.erman.usurf.ftp.service

import com.erman.usurf.ftp.data.FtpServerState
import com.erman.usurf.ftp.domain.FtpServerStatus

/**
 * Starts and stops the FTP server and is the only writer of [FtpServerState].
 *
 * Not thread-safe: the service calls it from its one worker thread only.
 */
class FtpServerController(private val serverState: FtpServerState, private val createEngine: (FtpServerConfig) -> FtpServerEngine) {
    private var engine: FtpServerEngine? = null

    /** Starts a server unless one is already running; returns the status after the call. */
    fun start(config: FtpServerConfig): FtpServerStatus {
        if (engine != null) return serverState.status.value
        serverState.update(FtpServerStatus.Starting)
        val newEngine: FtpServerEngine = createEngine(config)
        engine = newEngine
        try {
            val port: Int = newEngine.start()
            serverState.update(FtpServerStatus.Running(url = "${config.address}:$port", port = port))
        } catch (err: FtpServerStartException) {
            shutDown(reason = err)
        }
        return serverState.status.value
    }

    /** Stops the running server; without one it changes nothing, so a failure stays visible. */
    fun stop() {
        shutDown(reason = null)
    }

    private fun shutDown(reason: Throwable?) {
        val current: FtpServerEngine = engine ?: return
        engine = null
        if (reason == null) serverState.update(FtpServerStatus.Stopping)
        current.closeSessions()
        current.stop()
        serverState.update(if (reason == null) FtpServerStatus.Stopped else FtpServerStatus.Failed(describe(reason)))
    }

    private fun describe(reason: Throwable): String =
        generateSequence(reason) { it.cause }.mapNotNull { it.message }.lastOrNull() ?: reason.javaClass.simpleName
}
