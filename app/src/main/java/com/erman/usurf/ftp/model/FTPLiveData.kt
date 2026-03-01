package com.erman.usurf.ftp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.usurf.R
import com.erman.usurf.ftp.service.FtpServer
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge

class FTPLiveData(private val context: Context) : LiveData<Boolean>() {
    private val ftpServiceReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                postValue(isServiceRunning)
            }
        }

    override fun onActive() {
        super.onActive()
        LocalBroadcastManager.getInstance(context).registerReceiver(
            ftpServiceReceiver,
            IntentFilter(context.getString(R.string.ftp_broadcast_receiver)),
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ftpServiceReceiver)
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }
}

private val isServiceRunning: Boolean
    get() = FtpServer.isFtpServerRunning
