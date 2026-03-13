package com.erman.usurf.ftp.model

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.activity.MainActivity
import com.erman.usurf.R
import com.erman.usurf.application.MainApplication
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.utils.*
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.koin.android.ext.android.inject

class FtpServer : Service() {
    private val serverFactory = FtpServerFactory()
    private val server: FtpServer? = serverFactory.createServer()
    private val ftpPreferenceProvider: FtpPreferenceProvider by inject()

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
        connectionConfigFactory.isAnonymousLoginEnabled = true
        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(PORT_KEY, DEFAULT_PORT)

        val user = BaseUser()
        user.name = ftpPreferenceProvider.getUsername()
        user.password = ftpPreferenceProvider.getPassword()
        user.homeDirectory = ftpPreferenceProvider.getFtpPath()

        serverFactory.userManager.save(user)
        serverFactory.addListener("default", listenerFactory.createListener())
        server?.let {
            try {
                it.start()
                sendBroadcast()
                isFtpServerRunning = true
                logd("FTP server started")
            } catch (err: Exception) {
                loge("onStartCommand $err")

                it.stop()
                sendBroadcast()
                isFtpServerRunning = false
                removeNotification()
                logd("FTP server stopped")
            }
        }
        //displayNotification()
        return START_STICKY //will restart if the android system terminates it for any reason.
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

    @SuppressLint("InlinedApi")
    private fun getNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, true)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, FTP_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.ftp_server_is_running)).setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.ftp_notification_channel_name)
            val descriptionText = getString(R.string.ftp_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(FTP_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun removeNotification() {
        NotificationManagerCompat.from(this).cancel(FTP_NOTIFICATION_ID)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Intent(MainApplication.appContext, FtpServer()::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(FTP_NOTIFICATION_ID, getNotification())
            } else {
                startService(intent)
                with(NotificationManagerCompat.from(this)) {
                    notify(FTP_NOTIFICATION_ID, getNotification())
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Intent(MainApplication.appContext, FtpServer()::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(FTP_NOTIFICATION_ID, getNotification())
            } else {
                startService(intent)
                with(NotificationManagerCompat.from(this)) {
                    notify(FTP_NOTIFICATION_ID, getNotification())
                }
            }
        }
    }
}