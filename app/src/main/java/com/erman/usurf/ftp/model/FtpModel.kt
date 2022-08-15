package com.erman.usurf.ftp.model

import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.utils.logd
import java.net.Inet4Address
import java.net.NetworkInterface

class FtpModel {
    fun getIpAddress(): String {
        logd("Get ip address")
        var ip = ""
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { ip = it.hostAddress?.toString() ?: "" }
        }
        return "ftps://$ip"
    }

    fun startFTPServer() {
        Intent(appContext, FtpServer()::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
