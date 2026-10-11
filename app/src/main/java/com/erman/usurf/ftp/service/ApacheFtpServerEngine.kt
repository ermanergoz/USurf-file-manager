package com.erman.usurf.ftp.service

import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer as ApacheFtpServer
import org.apache.ftpserver.FtpServerConfigurationException
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.listener.Listener
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.mina.core.RuntimeIoException

private const val FTP_LISTENER_NAME: String = "default"
private const val ANONYMOUS_USER_NAME: String = "anonymous"

/** An Apache FtpServer with one listener and one user, built for a single start. */
class ApacheFtpServerEngine(private val config: FtpServerConfig) : FtpServerEngine {
    private var server: ApacheFtpServer? = null
    private var listener: Listener? = null

    override fun start(): Int {
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

    private fun createServer(listener: Listener): ApacheFtpServer {
        val serverFactory = FtpServerFactory()
        // Allow passwordless access only while the user keeps the default "anonymous" account;
        // once a real username is configured, credentials are enforced.
        serverFactory.connectionConfig =
            ConnectionConfigFactory()
                .apply { isAnonymousLoginEnabled = config.username == ANONYMOUS_USER_NAME }
                .createConnectionConfig()
        serverFactory.addListener(FTP_LISTENER_NAME, listener)
        val user = BaseUser()
        user.name = config.username
        user.password = config.password
        user.homeDirectory = config.homeDirectory
        serverFactory.userManager.save(user)
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
