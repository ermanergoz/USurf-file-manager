package com.erman.usurf.pushNotification.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PushNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.startService(Intent(context, PushNotificationIntentService::class.java))
    }
}