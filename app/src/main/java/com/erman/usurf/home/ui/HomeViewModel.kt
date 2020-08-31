package com.erman.usurf.home.ui

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.home.model.HomeModel

class HomeViewModel(private val homeModel: HomeModel) : ViewModel() {

    private val _storageButtons = MutableLiveData<MutableList<StorageButtonBinding>>().apply {
        value = homeModel.createStorageButtons()
    }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    fun onStorageButtonClick(view: View) {
        Log.e("clicked button", view.tag.toString())
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }
}