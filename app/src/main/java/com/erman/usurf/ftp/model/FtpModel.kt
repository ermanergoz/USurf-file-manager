package com.erman.usurf.ftp.model

import android.os.Environment
import com.erman.usurf.storage.domain.StorageKind
import com.erman.usurf.storage.domain.storageKindOf
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

private const val URL_SCHEME: String = "ftp://"

class FtpModel {
    fun getIpAddress(): String {
        var ip = ""
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { ip = it.hostAddress ?: "" }
        }
        // With no IPv4 address there is nothing a client could connect to.
        return if (ip.isEmpty()) "" else "$URL_SCHEME$ip"
    }

    fun storageKindOf(path: String): StorageKind = storageKindOf(path, getInternalStoragePath())

    private fun getInternalStoragePath(): String = try {
        File(Environment.getExternalStorageDirectory().absolutePath).canonicalPath
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        Environment.getExternalStorageDirectory().absolutePath
    }
}
