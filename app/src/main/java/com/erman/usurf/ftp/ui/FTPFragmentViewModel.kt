package com.erman.usurf.ftp.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.utils.Event
import com.erman.usurf.R
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.model.FTPLiveData
import com.erman.usurf.ftp.model.FtpServer
import com.erman.usurf.ftp.utils.DEFAULT_PORT
import com.erman.usurf.utils.loge

class FTPViewModel(private val ftpModel: FtpModel) : ViewModel() {
    private var preferenceProvider = FtpPreferenceProvider()

    val url = MutableLiveData<String>().apply {
        value = ftpModel.getIpAddress()
    }

    val username = MutableLiveData<String>().apply {
        value = preferenceProvider.getUsername()
    }

    val password = MutableLiveData<String>().apply {
        value = preferenceProvider.getPassword()
    }

    val port = MutableLiveData<String>().apply {
        value = preferenceProvider.getPort().toString()
    }

    val isConnectedToWifi = MutableLiveData<ConnectionLiveData>().apply {
        value = ConnectionLiveData()
    }
    val isServiceRunning = MutableLiveData<FTPLiveData>().apply {
        value = FTPLiveData()
    }

    fun onConnectClicked() {
        if (!FtpServer.isFtpServerRunning) ftpModel.startFTPServer()
        else ftpModel.stopFTPServer()
    }

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: MutableLiveData<Event<Int>> = _toastMessage

    fun onUsernameChanged(username: CharSequence, start: Int, before: Int, count: Int) {
        preferenceProvider.editUsername(username.toString())
    }

    fun onPasswordChanged(password: CharSequence, start: Int, before: Int, count: Int) {
        preferenceProvider.editPassword(password.toString())
    }

    fun onPortChanged(port: CharSequence, start: Int, before: Int, count: Int) {
        var newPort = DEFAULT_PORT
        try {
            newPort = port.toString().toInt()
        }catch (err: NumberFormatException)
        {
            loge("onPortChanged$err")
            newPort = DEFAULT_PORT
            _toastMessage.value = Event(R.string.port_error)
        }
        preferenceProvider.editPort(newPort)
    }
}