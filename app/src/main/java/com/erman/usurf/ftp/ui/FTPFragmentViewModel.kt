package com.erman.usurf.ftp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.erman.usurf.ftp.model.getIpAddress

class FTPFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val ipAddress: MutableLiveData<String>
        get() = _ipAddress

    private val _ipAddress = MutableLiveData<String>().apply {
        value = getIpAddress(application.applicationContext)
    }
}