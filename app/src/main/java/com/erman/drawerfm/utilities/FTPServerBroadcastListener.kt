package com.erman.drawerfm.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FTPServerBroadcastListener(private val onChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
            onChange.invoke()
    }
}