package com.erman.usurf.ftp.service

import com.erman.usurf.ftp.domain.FtpSettingError
import com.erman.usurf.ftp.domain.FtpSettingsRules
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer as ApacheFtpServer
import org.apache.ftpserver.FtpServerConfigurationException
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.ftplet.Ftplet
import org.apache.ftpserver.listener.Listener
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.apache.mina.core.RuntimeIoException

private const val FTP_LISTENER_NAME: String = "default"
private const val READ_ONLY_FTPLET_NAME: String = "readOnly"

/** An Apache FtpServer with one listener and one user, built for a single start. */
class ApacheFtpServerEngine(private val config: FtpServerConfig) : FtpServerEngine {
    private var server: ApacheFtpServer? = null
    private var listener: Listener? = null

    override fun start(): Int {
        refuseUnprotectedLogin()
        try {
            val newListener: Listener = ListenerFactory().apply { port = config.port }.createListener()
            listener = newListener
            val newServer: ApacheFtpServer = createServer(newListener)
            server = newServer
            newServer.start()
            return newListener.port
        } catch (err: FtpException) {
            throw FtpServerStartException(err)
        } catch (err: FtpServerConfigurationException) {
            throw FtpServerStartException(err)
        } catch (err: IllegalArgumentException) {
            throw FtpServerStartException(err)
        } catch (err: RuntimeIoException) {
            throw FtpServerStartException(err)
        }
    }

    // The screen never stores such settings; this keeps an older or edited preference file from opening the share.
    private fun refuseUnprotectedLogin() {
        val error: FtpSettingError =
            FtpSettingsRules.usernameError(config.username.orEmpty())
                ?: FtpSettingsRules.passwordError(config.password.orEmpty())
                ?: return
        val reason: String =
            when (error) {
                FtpSettingError.USERNAME_REQUIRED -> "a username is required"
                FtpSettingError.USERNAME_ANONYMOUS -> "anonymous login is not allowed"
                FtpSettingError.USERNAME_SPACE_AT_ENDS -> "a username cannot start or end with a space"
                FtpSettingError.USERNAME_CONTROL_CHARACTER -> "a username cannot contain a control character"
                FtpSettingError.PASSWORD_REQUIRED -> "a password is required"
                FtpSettingError.PASSWORD_SPACE_AT_ENDS -> "a password cannot start or end with a space"
                FtpSettingError.PASSWORD_CONTROL_CHARACTER -> "a password cannot contain a control character"
            }
        throw FtpServerStartException(IllegalArgumentException(reason))
    }

    private fun createServer(listener: Listener): ApacheFtpServer {
        val serverFactory: FtpServerFactory = buildServerFactory(config)
        serverFactory.addListener(FTP_LISTENER_NAME, listener)
        return serverFactory.createServer()
    }

    override fun closeSessions() {
        val activeListener: Listener = listener ?: return
        if (activeListener.isStopped) return
        activeListener.activeSessions.forEach { session ->
            session.dataConnection.closeDataConnection()
            session.closeNow()
        }
    }

    override fun stop() {
        server?.stop()
    }
}

/**
 * The server for [config] without its listener: one named user, a password always required, anonymous login off,
 * and read-only unless uploads are allowed.
 */
internal fun buildServerFactory(config: FtpServerConfig): FtpServerFactory {
    val serverFactory = FtpServerFactory()
    serverFactory.connectionConfig =
        ConnectionConfigFactory()
            .apply { isAnonymousLoginEnabled = false }
            .createConnectionConfig()
    val user = BaseUser()
    user.name = config.username
    user.password = config.password
    user.homeDirectory = config.homeDirectory
    if (config.allowUploads) {
        user.authorities = listOf(WritePermission())
    } else {
        // Mutable: the server clears this map when it stops.
        serverFactory.ftplets = mutableMapOf<String, Ftplet>(READ_ONLY_FTPLET_NAME to ReadOnlyFtplet())
    }
    serverFactory.userManager.save(user)
    return serverFactory
}
