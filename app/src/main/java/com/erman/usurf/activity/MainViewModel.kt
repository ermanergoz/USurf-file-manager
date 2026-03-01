package com.erman.usurf.activity

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.ROOT_DIRECTORY
import java.io.File

data class StorageSelectionResult(
    val showSafDialog: Boolean,
)

class MainViewModel(
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val storagePathsProvider: StoragePathsProvider,
) : ViewModel() {
    private val _storagePaths = MutableLiveData(storagePathsProvider.getStorageDirectories().toList())
    val storagePaths: LiveData<List<String>> = _storagePaths

    fun refreshStoragePaths() {
        _storagePaths.value = storagePathsProvider.getStorageDirectories().toList()
    }

    fun onStorageSelected(path: String): StorageSelectionResult {
        val showSafDialog =
            path != ROOT_DIRECTORY &&
                !File(path).canWrite() &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                storageDirectoryRepository.getChosenUri() == ""
        return StorageSelectionResult(showSafDialog = showSafDialog)
    }
}
