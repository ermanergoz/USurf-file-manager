package com.erman.usurf.ftp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.LiveData

class ConnectionLiveData(private val context: Context) : LiveData<Boolean>() {

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(context.isConnected)
        }
    }

    override fun onActive() {
        super.onActive()
        context.registerReceiver(
            networkReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            context.unregisterReceiver(networkReceiver)
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }
}

val Context.isConnected: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val connectivityManager =
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        val activeNetwork =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    } else {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo!!.isConnected
    }