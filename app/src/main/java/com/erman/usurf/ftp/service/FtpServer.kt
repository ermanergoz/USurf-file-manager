package com.erman.usurf.ftp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.erman.usurf.R
import com.erman.usurf.activity.MainActivity
import com.erman.usurf.ftp.data.FtpServerState
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.domain.FtpServerStatus
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.erman.usurf.utils.loge
import org.koin.android.ext.android.inject

private const val FTP_NOTIFICATION_CHANNEL_ID: String = "FtpNotificationChannel"
private const val PENDING_INTENT_REQUEST_CODE: Int = 0
private const val FTP_NOTIFICATION_ID: Int = 1
private const val WORKER_THREAD_NAME: String = "FtpServerWorker"

class FtpServer : Service() {
    private val ftpRepository: FtpRepository by inject()
    private val serverState: FtpServerState by inject()
    private val controller: FtpServerController by lazy { FtpServerController(serverState, ::ApacheFtpServerEngine) }
    private val mainHandler = Handler(Looper.getMainLooper())

    // Starting and stopping run here, one after the other: binding and closing sessions never block the main thread.
    private val worker = HandlerThread(WORKER_THREAD_NAME)
    private lateinit var workerHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        workerHandler.post { startServer(startId) }
        return START_STICKY
    }

    private fun startServer(startId: Int) {
        val status: FtpServerStatus = controller.start(readConfig())
        if (status is FtpServerStatus.Failed) {
            loge(status.reason)
            mainHandler.post { stopSelf(startId) }
        }
    }

    private fun readConfig(): FtpServerConfig = FtpServerConfig(
        address = ftpRepository.getIpAddress(),
        port = ftpRepository.getPort(),
        username = ftpRepository.getUsername(),
        password = ftpRepository.getPassword(),
        homeDirectory = ftpRepository.getFtpPath(),
        allowUploads = ftpRepository.isUploadAllowed()
    )

    override fun onDestroy() {
        super.onDestroy()
        workerHandler.post { controller.stop() }
        worker.quitSafely()
        stopServiceWithNotificationRemoval()
    }

    @SuppressLint("InlinedApi")
    private fun getNotification(): Notification {
        createNotificationChannel()
        val pendingIntent = createNotificationPendingIntent()
        return buildNotification(pendingIntent)
    }

    @SuppressLint("InlinedApi")
    private fun createNotificationPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, true)
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_IMMUTABLE
            }
        return PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    private fun buildNotification(pendingIntent: PendingIntent): Notification =
        NotificationCompat.Builder(this, FTP_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_folder)
            .setContentTitle(getString(R.string.ftp_server_is_running))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()

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

    private fun stopServiceWithNotificationRemoval() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        showForegroundNotification()
    }

    override fun onCreate() {
        super.onCreate()
        showForegroundNotification()
        worker.start()
        workerHandler = Handler(worker.looper)
    }

    // A foreground service on every API level: the server keeps running while the app is in the background.
    private fun showForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FTP_NOTIFICATION_ID,
                getNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FTP_NOTIFICATION_ID, getNotification())
        }
    }
}
