package com.erman.usurf.ftp.domain

sealed interface FtpServerStatus {
    data object Stopped : FtpServerStatus

    data object Starting : FtpServerStatus

    data class Running(val url: String, val port: Int) : FtpServerStatus

    data object Stopping : FtpServerStatus

    /**
     * A start that did not succeed; the server is not running.
     *
     * Not a data class on purpose: every failed attempt is a state of its own, so a second failure with
     * the same reason still reaches observers of a conflating StateFlow.
     */
    class Failed(val reason: String) : FtpServerStatus
}
