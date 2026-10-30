package com.erman.usurf.ftp.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/** Starts and stops the service the FTP server runs in, a foreground one where the platform asks for that. */
class FtpServiceLauncher(private val context: Context) {
    fun start() {
        val intent = Intent(context, FtpServer::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop() {
        val intent = Intent(context, FtpServer::class.java)
        context.stopService(intent)
    }
}
