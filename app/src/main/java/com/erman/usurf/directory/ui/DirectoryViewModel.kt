package com.erman.usurf.directory.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.directory.model.DirectoryModel

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is directory Fragment"
    }
    val text: LiveData<String> = _text
}