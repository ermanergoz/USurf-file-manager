package com.erman.usurf.ftp.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
        postValue(context.isConnected)
        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
        // Refused without the network state permission; the screen then keeps the state read above.
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (err: SecurityException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }

    override fun onInactive() {
        super.onInactive()
        // Refused for a callback that was never registered, when registering it was refused.
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (err: IllegalArgumentException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }
}

val Context.isConnected: Boolean
    get() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    }
