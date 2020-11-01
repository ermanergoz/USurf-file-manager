package com.erman.usurf.pushNotification.model

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.erman.usurf.R
import com.erman.usurf.activity.MainActivity
import com.erman.usurf.pushNotification.utils.*

class PushNotificationIntentService : IntentService(INTENT_SERVICE_NAME) {
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.push_notification_channel_description)
            val descriptionText = getString(R.string.push_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(PUSH_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(KEY_INTENT_IS_CLEANING_NOTIFICATION_CLICKED, true)

        val pendingIntent =
            PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder =
            NotificationCompat.Builder(this, PUSH_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.free_up_space))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            val notification = builder.build()
            notification.flags = Notification.FLAG_AUTO_CANCEL
            notify(PUSH_NOTIFICATION_ID, notification)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        createNotificationChannel()
        createNotification()
    }
}