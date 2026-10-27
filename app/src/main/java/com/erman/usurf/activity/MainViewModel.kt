package com.erman.usurf.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoriesSource
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.Event

class MainViewModel(
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val storagePathsProvider: StoragePathsProvider,
    private val storageAccessPolicy: StorageAccessPolicy,
    storageDirectoriesSource: StorageDirectoriesSource
) : ViewModel() {
    private val _uiEvents = MutableLiveData<Event<MainUiEvent>>()

    /** One picker for every screen that asks for SD card access, so its grant has one way back. */
    val uiEvents: LiveData<Event<MainUiEvent>> = _uiEvents

    /** The drawer's storage entries: read off the main thread, and again when root access is turned on or off. */
    val storagePaths: LiveData<List<String>> = storageDirectoriesSource.storageDirectories.map { it.toList() }

    fun onSafUriChosen(uri: String) {
        storageDirectoryRepository.setChosenUri(uri)
    }

    /** Whether Preferences offers SD card access: while a volume is present whose writes need a granted tree. */
    fun canOfferSafGrant(): Boolean = storagePathsProvider.getStorageDirectories().any(storageAccessPolicy::canGrantSafAccess)

    /** Grant SD card access, asked for by a screen: its storage card, a write the card refused, or Preferences. */
    fun requestSafAccess() {
        _uiEvents.value = Event(MainUiEvent.LaunchSafPicker)
    }

    /** A storage volume chosen in the drawer: a card no tree of which is held asks for one while its folder opens. */
    fun onStorageSelected(path: String) {
        if (storageAccessPolicy.needsFirstSafGrant(path)) requestSafAccess()
    }
}
