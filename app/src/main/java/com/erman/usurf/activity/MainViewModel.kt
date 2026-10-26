package com.erman.usurf.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoriesSource
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider

data class StorageSelectionResult(val showSafDialog: Boolean)

class MainViewModel(
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val storagePathsProvider: StoragePathsProvider,
    private val storageAccessPolicy: StorageAccessPolicy,
    storageDirectoriesSource: StorageDirectoriesSource
) : ViewModel() {
    /** The drawer's storage entries: read off the main thread, and again when root access is turned on or off. */
    val storagePaths: LiveData<List<String>> = storageDirectoriesSource.storageDirectories.map { it.toList() }

    fun onSafUriChosen(uri: String) {
        storageDirectoryRepository.setChosenUri(uri)
    }

    /** Whether Preferences offers SD card access: while a volume is present whose writes need a granted tree. */
    fun canOfferSafGrant(): Boolean = storagePathsProvider.getStorageDirectories().any(storageAccessPolicy::canGrantSafAccess)

    fun onStorageSelected(path: String): StorageSelectionResult =
        StorageSelectionResult(showSafDialog = storageAccessPolicy.needsFirstSafGrant(path))
}
