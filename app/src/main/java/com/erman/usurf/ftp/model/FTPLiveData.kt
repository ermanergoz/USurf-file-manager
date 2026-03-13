package com.erman.usurf.ftp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.R

class FTPLiveData : LiveData<Boolean>() {

    private val ftpServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(isServiceRunning)
        }
    }

    override fun onActive() {
        super.onActive()
        LocalBroadcastManager.getInstance(appContext).registerReceiver(
            ftpServiceReceiver,
            IntentFilter(appContext.getString(R.string.ftp_broadcast_receiver))
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            LocalBroadcastManager.getInstance(appContext).unregisterReceiver(ftpServiceReceiver)
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }
}

val isServiceRunning: Boolean
    get() = FtpServer.isFtpServerRunning
