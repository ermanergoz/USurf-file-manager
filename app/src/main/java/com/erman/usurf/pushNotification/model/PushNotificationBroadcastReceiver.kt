package com.erman.usurf.pushNotification.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.erman.usurf.utils.loge

class PushNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            context.startService(Intent(context, PushNotificationIntentService::class.java))
        } catch (err: Exception) {
            loge("onReceive $err")
        }
    }
}