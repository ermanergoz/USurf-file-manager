package com.erman.drawerfm.utilities

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.drawerfm.R
import com.erman.drawerfm.common.DEFAULT_PORT
import com.erman.drawerfm.common.DEFAULT_USER_NAME
import com.erman.drawerfm.common.KEY_INTENT_CHOSEN_PATH
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser

class FTPServer() : Service() {

    private val serverFactory = FtpServerFactory()
    private val server: FtpServer? = serverFactory.createServer()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { // Let it continue running until it is stopped.
        Toast.makeText(this, getString(R.string.connected), Toast.LENGTH_LONG).show()

        val listenerFactory = ListenerFactory()
        val connectionConfigFactory = ConnectionConfigFactory()
        connectionConfigFactory.isAnonymousLoginEnabled = true

        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port = DEFAULT_PORT

        val user = BaseUser()
        user.name = DEFAULT_USER_NAME
        user.homeDirectory = intent!!.getStringExtra(KEY_INTENT_CHOSEN_PATH)
        serverFactory.userManager.save(user)

        serverFactory.addListener("default", listenerFactory.createListener())

        //server = serverFactory.createServer()
        server!!.start()

        sendBroadcast()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server!!.stop()
        sendBroadcast()
        Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_LONG).show()
    }

    private fun sendBroadcast() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.ftp_broadcast_receiver)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }
}