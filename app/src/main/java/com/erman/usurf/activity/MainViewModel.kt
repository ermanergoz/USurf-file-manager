package com.erman.usurf.activity

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.activity.utils.EMPTY_STR
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.StoragePaths
import java.io.File

data class StorageSelectionResult(
    val showSafDialog: Boolean,
)

class MainViewModel(
    private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
) : ViewModel() {
    private val _storagePaths = MutableLiveData(StoragePaths.getStorageDirectories().toList())
    val storagePaths: LiveData<List<String>> = _storagePaths

    fun refreshStoragePaths() {
        _storagePaths.value = StoragePaths.getStorageDirectories().toList()
    }

    fun onStorageSelected(path: String): StorageSelectionResult {
        val showSafDialog =
            path != ROOT_DIRECTORY &&
                !File(path).canWrite() &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                storageDirectoryPreferenceProvider.getChosenUri() == EMPTY_STR
        return StorageSelectionResult(showSafDialog = showSafDialog)
    }
}
