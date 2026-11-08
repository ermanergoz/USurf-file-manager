package com.erman.usurf.ftp.service

import java.util.Locale
import org.apache.ftpserver.ftplet.DefaultFtpReply
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpReply
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletResult

private const val UPLOADS_OFF_MESSAGE: String = "Uploads are turned off on this server."

// Every command that changes the shared storage. Apache FtpServer checks the write permission only for some of
// them (MFMT changes a file's time without it), so the read-only server answers all of them here.
private val WRITE_COMMANDS: Set<String> =
    setOf("APPE", "DELE", "MFMT", "MKD", "RMD", "RNFR", "RNTO", "SITE", "STOR", "STOU", "XMKD", "XRMD")

/** Answers 550 to every command that writes, before the server runs it; installed while uploads are off. */
class ReadOnlyFtplet : DefaultFtplet() {
    override fun beforeCommand(session: FtpSession, request: FtpRequest): FtpletResult {
        if (request.command.uppercase(Locale.ROOT) !in WRITE_COMMANDS) return FtpletResult.DEFAULT
        session.write(DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, UPLOADS_OFF_MESSAGE))
        return FtpletResult.SKIP
    }
}
