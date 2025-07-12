package com.erman.usurf.ftp.model

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.erman.usurf.ftp.utils.URL_SCHEME
import com.erman.usurf.utils.logd
import java.net.Inet4Address
import java.net.NetworkInterface

class FtpModel(private val context: Context) {
    fun getIpAddress(): String {
        logd("Get ip address")
        var ip = ""
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { ip = it.hostAddress?.toString() ?: "" }
        }
        return "$URL_SCHEME$ip"
    }

    fun startFTPServer() {
        Intent(context, FtpServer()::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun stopFTPServer() {
        Intent(context, FtpServer()::class.java).also { intent ->
            context.stopService(intent)
        }
    }
}
