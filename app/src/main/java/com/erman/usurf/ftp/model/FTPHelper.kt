package com.erman.usurf.ftp.model

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

fun getIpAddress(applicationContext: Context): String {
    val wifiManager =
        applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager  //applicationContext is to avoid memory leak
    val wifiInfo = wifiManager.connectionInfo
    val ip = wifiInfo.ipAddress
    Log.e("ip", String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff))
    return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    //Formatter.formatIpAddress is deprecated beacuse it doesnt work with ipv6
}
