package com.erman.usurf.ftp.service

import com.erman.usurf.ftp.data.FtpServerState
import com.erman.usurf.ftp.domain.FtpServerStatus

/**
 * Starts and stops the FTP server and is the only writer of [FtpServerState].
 *
 * Not thread-safe: the service calls it from its one worker thread only. Nothing it calls may end that thread, so
 * neither [start] nor [stop] throws: every failure ends as [FtpServerStatus.Failed].
 */
class FtpServerController(private val serverState: FtpServerState, private val createEngine: (FtpServerConfig) -> FtpServerEngine) {
    private var engine: FtpServerEngine? = null

    /** Starts a server on the settings [readConfig] returns, unless one is already running; returns the status after the call. */
    fun start(readConfig: () -> FtpServerConfig): FtpServerStatus {
        if (engine != null) return serverState.status.value
        serverState.update(FtpServerStatus.Starting)
        try {
            val config: FtpServerConfig = readConfig()
            val newEngine: FtpServerEngine = createEngine(config)
            engine = newEngine
            val port: Int = newEngine.start()
            serverState.update(FtpServerStatus.Running(url = "${config.address}:$port", port = port))
        } catch (err: Exception) {
            // Any failure, reading the settings included: an exception leaving the worker would end the process, and the
            // sticky service would restart into it. The start failure is the reason shown, not a later one from releasing.
            val current: FtpServerEngine? = engine
            engine = null
            current?.let(::release)
            serverState.update(FtpServerStatus.Failed(describe(err)))
        }
        return serverState.status.value
    }

    /** Stops the running server; without one it changes nothing, so a failure stays visible. */
    fun stop() {
        val current: FtpServerEngine = engine ?: return
        engine = null
        serverState.update(FtpServerStatus.Stopping)
        val failure: Exception? = release(current)
        serverState.update(if (failure == null) FtpServerStatus.Stopped else FtpServerStatus.Failed(describe(failure)))
    }

    /** Closes the sessions, then stops the server even when closing failed; returns the first failure instead of throwing it. */
    private fun release(current: FtpServerEngine): Exception? {
        val closeFailure: Exception? = attempt(current::closeSessions)
        val stopFailure: Exception? = attempt(current::stop)
        return closeFailure ?: stopFailure
    }

    private fun attempt(action: () -> Unit): Exception? = try {
        action()
        null
    } catch (err: Exception) {
        err
    }

    private fun describe(reason: Throwable): String =
        generateSequence(reason) { it.cause }.mapNotNull { it.message }.lastOrNull() ?: reason.javaClass.simpleName
}
