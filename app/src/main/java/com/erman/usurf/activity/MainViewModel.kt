package com.erman.usurf.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider

data class StorageSelectionResult(val showSafDialog: Boolean)

class MainViewModel(
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val storagePathsProvider: StoragePathsProvider,
    private val storageAccessPolicy: StorageAccessPolicy
) : ViewModel() {
    private val _storagePaths = MutableLiveData(storagePathsProvider.getStorageDirectories().toList())
    val storagePaths: LiveData<List<String>> = _storagePaths

    fun refreshStoragePaths() {
        _storagePaths.value = storagePathsProvider.getStorageDirectories().toList()
    }

    fun onSafUriChosen(uri: String) {
        storageDirectoryRepository.setChosenUri(uri)
    }

    /** Whether Preferences offers SD card access: while a volume is present whose writes need a granted tree. */
    fun canOfferSafGrant(): Boolean = storagePathsProvider.getStorageDirectories().any(storageAccessPolicy::canGrantSafAccess)

    fun onStorageSelected(path: String): StorageSelectionResult =
        StorageSelectionResult(showSafDialog = storageAccessPolicy.needsSafGrant(path))
}
