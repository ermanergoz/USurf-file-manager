package com.erman.usurf.ftp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.app.MainApplication.Companion.appContext
import com.erman.usurf.R
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge

class FTPLiveData : LiveData<Boolean>() {

    private val ftpServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(isServiceRunning)
        }
    }

    override fun onActive() {
        super.onActive()
        logd("Register ftpServiceReceiver")
        LocalBroadcastManager.getInstance(appContext).registerReceiver(
            ftpServiceReceiver,
            IntentFilter(appContext.getString(R.string.ftp_broadcast_receiver))
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            logd("Unregister ftpServiceReceiver")
            LocalBroadcastManager.getInstance(appContext).unregisterReceiver(ftpServiceReceiver)
        } catch (err: Exception) {
            loge("onInactive $err")
        }
    }
}

val isServiceRunning: Boolean
    get() = FtpServer.isFtpServerRunning
