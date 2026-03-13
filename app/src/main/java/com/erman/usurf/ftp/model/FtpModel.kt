package com.erman.usurf.ftp.model

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.erman.usurf.R
import com.erman.usurf.ftp.service.FtpServer
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

private const val URL_SCHEME: String = "ftps://"

class FtpModel(private val context: Context) {
    fun getIpAddress(): String {
        var ip = ""
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { ip = it.hostAddress?.toString() ?: "" }
        }
        return "$URL_SCHEME$ip"
    }

    fun getStorageDisplayName(path: String): String {
        val internalPath: String = getInternalStoragePath()
        return when (path) {
            ROOT_DIRECTORY -> context.getString(R.string.root_storage)
            internalPath -> context.getString(R.string.internal_storage)
            else -> context.getString(R.string.sd_card)
        }
    }

    fun isExternalStorage(path: String): Boolean {
        val internalPath: String = getInternalStoragePath()
        return path != ROOT_DIRECTORY && path != internalPath
    }

    private fun getInternalStoragePath(): String {
        return try {
            File(Environment.getExternalStorageDirectory().absolutePath).canonicalPath
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            Environment.getExternalStorageDirectory().absolutePath
        }
    }

    fun startFTPServer() {
        val intent = Intent(context, FtpServer::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopFTPServer() {
        val intent = Intent(context, FtpServer::class.java)
        context.stopService(intent)
    }
}
