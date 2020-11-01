package com.erman.usurf.ftp.model

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.utils.logd

class FtpModel {
    fun getIpAddress(): String {
        logd("Get ip address")
        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        //applicationContext is to avoid memory leak
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress

        return "ftps://" + String.format("%d.%d.%d.%d",
            ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        //Formatter.formatIpAddress is deprecated beacuse it doesnt work with ipv6
    }

    fun startFTPServer() {
        Intent(appContext, FtpServer()::class.java).also { intent ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, intent)
            } else
                appContext.startService(intent)
        }
    }

    fun stopFTPServer() {
        Intent(appContext, FtpServer()::class.java).also { intent ->
            appContext.stopService(intent)
        }
    }
}