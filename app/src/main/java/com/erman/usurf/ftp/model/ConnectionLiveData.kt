package com.erman.usurf.ftp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.LiveData
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge

class ConnectionLiveData : LiveData<Boolean>() {
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(context.isConnected)
        }
    }

    override fun onActive() {
        super.onActive()
        logd("Register networkReceiver")
        appContext.registerReceiver(
            networkReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            logd("Unregister networkReceiver")
            appContext.unregisterReceiver(networkReceiver)
        } catch (err: Exception) {
            loge("onInactive $err")
        }
    }
}

val Context.isConnected: Boolean?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val connectivityManager =
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        val activeNetwork =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    } else {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected
    }