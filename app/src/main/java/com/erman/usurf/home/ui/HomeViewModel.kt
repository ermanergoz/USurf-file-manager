package com.erman.usurf.home.ui

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.utils.Event

class HomeViewModel(private val homeModel: HomeModel) : ViewModel() {

    private val _storageButtons = MutableLiveData<MutableList<StorageButtonBinding>>().apply {
        value = homeModel.createStorageButtons()
    }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    private val _openTaskEvent = MutableLiveData<Event<Int>>()
    val openTaskEvent: MutableLiveData<Event<Int>> = _openTaskEvent

    private val _storagePath = MutableLiveData<String>()
    val storagePath: MutableLiveData<String> = _storagePath

    fun onStorageButtonClick(view: View) {
        _storagePath.value = view.tag.toString()
        _openTaskEvent.value = Event(R.id.action_nav_home_to_nav_directory)
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }
}