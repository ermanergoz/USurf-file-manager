package com.erman.usurf.home.ui

import android.os.Build
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.UIEventArgs
import com.erman.usurf.utils.DirectoryPreferenceProvider
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.utils.Event
import java.io.File

class HomeViewModel(private val homeModel: HomeModel) : ViewModel() {

    private val _storageButtons = MutableLiveData<MutableList<StorageButtonBinding>>().apply {
        value = homeModel.createStorageButtons()
    }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    private val _navigateToDirectory = MutableLiveData<Event<Int>>()
    val navigateToDirectory: MutableLiveData<Event<Int>> = _navigateToDirectory

    private val _storagePath = MutableLiveData<String>()
    val storagePath: MutableLiveData<String> = _storagePath

    private val _saf = MutableLiveData<Event<UIEventArgs.SAFActivityArgs>>()
    val saf: MutableLiveData<Event<UIEventArgs.SAFActivityArgs>> = _saf

    fun onStorageButtonClick(view: View) {
        _storagePath.value = view.tag.toString()
        _navigateToDirectory.value = Event(R.id.action_nav_home_to_nav_directory)
        if (!File(storagePath.value!!).canWrite() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            _saf.value = Event(UIEventArgs.SAFActivityArgs)
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }

    fun saveDocumentTree(treeUri: String) {
        DirectoryPreferenceProvider().editChosenUri(treeUri)
    }
}