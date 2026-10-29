package com.erman.usurf.ftp.data

import com.erman.usurf.ftp.domain.FtpSettingsRepository

class FtpSettingsRepositoryImpl(private val ftpPreferenceProvider: FtpPreferenceProvider) : FtpSettingsRepository {
    override fun getUsername(): String = ftpPreferenceProvider.getUsername()

    override fun getPassword(): String = ftpPreferenceProvider.getPassword()

    override fun getPort(): Int = ftpPreferenceProvider.getPort()

    override fun getFtpPath(): String? = ftpPreferenceProvider.getFtpPath()

    override fun isUploadAllowed(): Boolean = ftpPreferenceProvider.isUploadAllowed()

    override fun editUsername(username: String) {
        ftpPreferenceProvider.editUsername(username)
    }

    override fun editPassword(password: String) {
        ftpPreferenceProvider.editPassword(password)
    }

    override fun editPort(port: Int) {
        ftpPreferenceProvider.editPort(port)
    }

    override fun editFtpPath(path: String?) {
        ftpPreferenceProvider.editFtpPath(path)
    }

    override fun editUploadAllowed(allowed: Boolean) {
        ftpPreferenceProvider.editUploadAllowed(allowed)
    }
}
