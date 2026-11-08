package com.erman.usurf.ftp.domain

/** The FTP server's stored settings: who may log in, on which port, which folder it shares and whether uploads are on. */
interface FtpSettingsRepository {
    /** The username a client logs in with; set on the first read of a new install, so never missing. */
    fun getUsername(): String

    /** The password a client logs in with; generated on the first read of a new install, so never missing. */
    fun getPassword(): String

    fun getPort(): Int

    /** The folder the server shares; the first storage directory until one is chosen, none when there is none. */
    fun getFtpPath(): String?

    fun isUploadAllowed(): Boolean

    fun editUsername(username: String)

    fun editPassword(password: String)

    fun editPort(port: Int)

    fun editFtpPath(path: String?)

    fun editUploadAllowed(allowed: Boolean)
}
