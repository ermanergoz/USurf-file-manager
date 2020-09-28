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

class FtpServer : Service() {
    private val serverFactory = FtpServerFactory()
    private val server: FtpServer? = serverFactory.createServer()

    companion object {
        var isFtpServerRunning: Boolean = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("Start FTP server service")
        // Let it continue running until it is stopped.
        val listenerFactory = ListenerFactory()
        val connectionConfigFactory = ConnectionConfigFactory()
        //connectionConfigFactory.isAnonymousLoginEnabled = true
        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port = DEFAULT_PORT

        val user = BaseUser()
        getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(
            USERNAME_KEY, DEFAULT_USER_NAME)?.let {
            user.name = it
        }
        getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(
            PASSWORD_KEY, PASSWORD_DEF_VAL)?.let {
            user.password = it
        }
        user.homeDirectory = "storage/emulated/0"/*intent!!.getStringExtra(KEY_INTENT_CHOSEN_PATH)*/

        serverFactory.userManager.save(user)
        serverFactory.addListener("default", listenerFactory.createListener())
        server?.let {
            it.start()
            sendBroadcast()
            isFtpServerRunning = true
            logd("FTP server started")
        }

        return START_STICKY //will restart if the android system terminates for any reason.
    }

    override fun onDestroy() {
        logd("Stop FTP server service")
        super.onDestroy()
        server?.let {
            it.stop()
            sendBroadcast()
            isFtpServerRunning = false
            logd("FTP server stopped")
        }
    }

    private fun sendBroadcast() {
        logd("Send broadcast")
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.ftp_broadcast_receiver)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }
}