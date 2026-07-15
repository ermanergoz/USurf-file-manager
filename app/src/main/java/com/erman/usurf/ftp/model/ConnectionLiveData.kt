package com.erman.usurf.ftp.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge

class ConnectionLiveData(private val context: Context) : LiveData<Boolean>() {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                postValue(true)
            }

            override fun onLost(network: Network) {
                postValue(false)
            }
        }

    override fun onActive() {
        super.onActive()
        postValue(context.isConnected ?: false)
        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }

    override fun onInactive() {
        super.onInactive()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }
}

val Context.isConnected: Boolean?
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectivityManager =
                (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        } else {
            @Suppress("DEPRECATION")
            (
                getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as? ConnectivityManager
                )?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected
        }
