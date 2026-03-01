package com.erman.usurf.ftp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.R
import com.erman.usurf.activity.MainActivity
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.ftp.utils.PORT_KEY
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.erman.usurf.utils.SHARED_PREF_FILE
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.koin.android.ext.android.inject
import org.apache.ftpserver.FtpServer as ApacheFtpServer

private const val FTP_LISTENER_NAME: String = "default"
private const val FTP_NOTIFICATION_CHANNEL_ID: String = "FtpNotificationChannel"
private const val PENDING_INTENT_REQUEST_CODE = 0
private const val FTP_NOTIFICATION_ID: Int = 1

class FtpServer : Service() {
    private val serverFactory = FtpServerFactory()
    private val server: ApacheFtpServer? = serverFactory.createServer()
    private val ftpPreferenceProvider: FtpPreferenceProvider by inject()

    companion object {
        var isFtpServerRunning: Boolean = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Let it continue running until it is stopped.
        val listenerFactory = ListenerFactory()
        val connectionConfigFactory = ConnectionConfigFactory()
        connectionConfigFactory.isAnonymousLoginEnabled = true
        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()
        listenerFactory.port =
            getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(
                PORT_KEY,
                DEFAULT_PORT,
            )

        val user = BaseUser()
        user.name = ftpPreferenceProvider.getUsername()
        user.password = ftpPreferenceProvider.getPassword()
        user.homeDirectory = ftpPreferenceProvider.getFtpPath()

        serverFactory.userManager.save(user)
        serverFactory.addListener(FTP_LISTENER_NAME, listenerFactory.createListener())
        server?.let {
            try {
                it.start()
                sendBroadcast()
                isFtpServerRunning = true
            } catch (err: Exception) {
                err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                it.stop()
                sendBroadcast()
                isFtpServerRunning = false
                stopServiceWithNotificationRemoval()
                stopSelf()
            }
        }
        return START_STICKY // will restart if the android system terminates it for any reason.
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.let {
            it.stop()
            sendBroadcast()
            isFtpServerRunning = false
        }
        stopServiceWithNotificationRemoval()
    }

    private fun sendBroadcast() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.ftp_broadcast_receiver)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(broadcastIntent)
    }

    @SuppressLint("InlinedApi")
    private fun getNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, true)

        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    this,
                    PENDING_INTENT_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    PENDING_INTENT_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val builder =
            NotificationCompat.Builder(this, FTP_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_folder)
                .setContentTitle(getString(R.string.ftp_server_is_running))
                .setContentIntent(pendingIntent)
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
            val channel =
                NotificationChannel(FTP_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Stops foreground state (removing the notification on O+) and cancels the notification on pre-O.
     * Call when the service is stopping (failure path or onDestroy).
     */
    private fun stopServiceWithNotificationRemoval() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } else {
            NotificationManagerCompat.from(this).cancel(FTP_NOTIFICATION_ID)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FTP_NOTIFICATION_ID,
                    getNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(FTP_NOTIFICATION_ID, getNotification())
            }
        } else {
            showNotificationIfPermitted()
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FTP_NOTIFICATION_ID,
                    getNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(FTP_NOTIFICATION_ID, getNotification())
            }
        } else {
            showNotificationIfPermitted()
        }
    }

    private fun showNotificationIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        NotificationManagerCompat.from(this).notify(FTP_NOTIFICATION_ID, getNotification())
    }
}
