package com.erman.usurf.ftp.model

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.*
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.utils.*
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser

class FTPServer : Service() {
    private val serverFactory = FtpServerFactory()
    private val server: FtpServer? = serverFactory.createServer()

    companion object {
        var isFtpServerRunning: Boolean = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int { // Let it continue running until it is stopped.
        val listenerFactory = ListenerFactory()
        val connectionConfigFactory = ConnectionConfigFactory()
        //connectionConfigFactory.isAnonymousLoginEnabled = true
        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port = DEFAULT_PORT

        val user = BaseUser()
        user.name = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(
            USERNAME_KEY,
            DEFAULT_USER_NAME
        )!!
        user.password = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(
            PASSWORD_KEY,
            PASSWORD_DEF_VAL
        )!!
        user.homeDirectory = "storage/emulated/0"/*intent!!.getStringExtra(KEY_INTENT_CHOSEN_PATH)*/

        serverFactory.userManager.save(user)
        serverFactory.addListener("default", listenerFactory.createListener())
        server!!.start()
        sendBroadcast()
        isFtpServerRunning = true

        return START_STICKY //will restart if the android system terminates for any reason.
    }

    override fun onDestroy() {
        super.onDestroy()
        server!!.stop()
        sendBroadcast()
        isFtpServerRunning = false
        Log.i("DefaultFtpServer", "FTP server stopped")
    }

    private fun sendBroadcast() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.ftp_broadcast_receiver)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }
}