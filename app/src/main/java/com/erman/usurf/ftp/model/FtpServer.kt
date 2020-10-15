package com.erman.usurf.ftp.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.MainActivity
import com.erman.usurf.R
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd
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
        user.homeDirectory = FtpPreferenceProvider().getFtpPath()

        serverFactory.userManager.save(user)
        serverFactory.addListener("default", listenerFactory.createListener())
        server?.let {
            it.start()
            sendBroadcast()
            isFtpServerRunning = true
            logd("FTP server started")
        }
        displayNotification()
        return START_STICKY //will restart if the android system terminates for any reason.
    }

    override fun onDestroy() {
        logd("Stop FTP server service")
        super.onDestroy()
        server?.let {
            it.stop()
            sendBroadcast()
            isFtpServerRunning = false
            removeNotification()
            logd("FTP server stopped")
        }
    }

    private fun sendBroadcast() {
        logd("Send broadcast")
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.ftp_broadcast_receiver)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }

    private fun displayNotification() {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, true)

        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_ftp_notification)
                .setContentTitle(getString(R.string.ftp_server_is_running))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun removeNotification() {
        NotificationManagerCompat.from(this).cancel(1)
    }
}