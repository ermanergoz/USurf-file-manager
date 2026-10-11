package com.erman.usurf.ftp.data

import com.erman.usurf.ftp.domain.FtpServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one record of whether the FTP server runs, shared by the process.
 *
 * Only the FTP service writes it (through its server controller); everything else reads [status].
 */
class FtpServerState {
    private val mutableStatus = MutableStateFlow<FtpServerStatus>(FtpServerStatus.Stopped)
    val status: StateFlow<FtpServerStatus> = mutableStatus.asStateFlow()

    fun update(status: FtpServerStatus) {
        mutableStatus.value = status
    }
}
