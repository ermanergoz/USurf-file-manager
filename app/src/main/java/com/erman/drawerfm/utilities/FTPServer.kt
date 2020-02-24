package com.erman.drawerfm.utilities

import android.os.AsyncTask
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser

class FTPServer(var chosenPath: String) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String {
        val serverFactory = FtpServerFactory()
        val listenerFactory = ListenerFactory()
        val connectionConfigFactory = ConnectionConfigFactory()
        connectionConfigFactory.isAnonymousLoginEnabled = true

        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port = 2221


        val user = BaseUser()
        user.setName("anonymous")
        user.homeDirectory = chosenPath
        serverFactory.userManager.save(user)

        serverFactory.addListener("default", listenerFactory.createListener())

        val server = serverFactory.createServer()
        server.start()

        return ""
    }
}