package com.erman.drawerfm.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FileBroadcastReceiver(val path: String, private val onChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val filePath = intent?.extras?.getString("path for broadcast")
        if (filePath.equals(path)) {
            onChange.invoke()
        }
    }
}