package com.erman.usurf.home.ui

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.erman.usurf.R
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.home.data.HomePreferenceProvider
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.logi
import io.realm.Realm
import java.io.File

class HomeViewModel(
    private val homeModel: HomeModel,
    private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
    private val homePreferenceProvider: HomePreferenceProvider,
    private val realm: Realm,
    private val favoriteDao: FavoriteDao,
) : ViewModel() {
    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private val _uiEvents = MutableLiveData<Event<HomeUiEvent>>()
    val uiEvents: LiveData<Event<HomeUiEvent>> = _uiEvents

    private val _storageItems =
        MutableLiveData<List<StorageItem>>().apply {
            value = homeModel.getStorageItems()
        }
    val storageItems: LiveData<List<StorageItem>> = _storageItems

    val favorites: LiveData<List<FavoriteItem>> =
        favoriteDao.getFavorites().map { realmResults ->
            realm.copyFromRealm(realmResults).map { favorite ->
                FavoriteItem(
                    id = favorite.id,
                    name = favorite.name,
                    path = favorite.path,
                )
            }
        }

    private fun updateState(transform: (HomeUiState) -> HomeUiState) {
        _uiState.value = transform(_uiState.value ?: HomeUiState())
    }

    fun onStorageButtonClick(path: String) {
        val newPath = path
        updateState { it.copy(path = newPath) }
        _uiEvents.value = Event(HomeUiEvent.NavigateToDirectory(R.id.global_action_nav_directory, newPath))
        if (shouldShowSafDialog(newPath)) {
            _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.SAFActivityArgs))
        }
        val isKitkatRemovableStorage = (
            Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT &&
                StoragePaths.getStorageDirectories().size > 1 &&
                newPath == StoragePaths.getStorageDirectories().elementAt(1)
        )
        val showWarning =
            isKitkatRemovableStorage &&
                !homePreferenceProvider.getIsKitkatRemovableStorageWarningDisplayedPreference()
        if (showWarning) {
            _uiEvents.value =
                Event(
                    HomeUiEvent.ShowDialog(
                        DialogArgs.KitkatRemovableStorageDialogArgs(isKitkatRemovableStorage),
                    ),
                )
            homePreferenceProvider.editIsKitkatRemovableStorageWarningDisplayedPreference(true)
        }
    }

    private fun shouldShowSafDialog(path: String): Boolean {
        if (path == ROOT_DIRECTORY) return false
        if (File(path).canWrite()) return false
        return when {
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> false
            storageDirectoryPreferenceProvider.getChosenUri() != "" -> false
            else -> true
        }
    }

    fun getUsedStoragePercentage(path: String): Int {
        return homeModel.getUsedStoragePercentage(path)
    }

    fun onFavoriteAdd(
        path: String,
        name: String,
    ) {
        logi("Add favorite: $name")
        if (favoriteDao.addFavorite(path, name)) {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.favorite_created))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.unable_to_create_favorite))
        }
    }

    fun onFavoriteClick(favoritePath: String) {
        if (File(favoritePath).exists()) {
            if (File(favoritePath).isDirectory) {
                logd("Open a favorite directory")
                updateState { it.copy(path = favoritePath) }
                _uiEvents.value =
                    Event(
                        HomeUiEvent.NavigateToDirectory(
                            R.id.global_action_nav_directory,
                            favoritePath,
                        ),
                    )
            } else {
                _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.OpenFileActivityArgs(favoritePath)))
            }
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.invalid_favorite))
        }
    }

    fun onFavoriteLongClick(
        favoritePath: String,
        favoriteName: String,
    ): Boolean {
        _uiEvents.value =
            Event(
                HomeUiEvent.ShowDialog(
                    DialogArgs.FavoriteOptionsDialogArgs(favoritePath, favoriteName),
                ),
            )
        return true
    }

    fun deleteFavorite(favoritePath: String) {
        logi("Delete favorite: $favoritePath")
        if (favoriteDao.removeFavorite(favoritePath)) {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.favorite_deleted))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.unable_to_delete_favorite))
        }
    }

    fun setRenameMode() {
        updateState { it.copy(isRenameMode = true) }
    }

    fun turnOffRenameMode() {
        updateState { it.copy(isRenameMode = false) }
    }

    fun onRenameFavoriteOkPressed(
        favoritePath: String,
        newName: String,
    ) {
        logi("Rename favorite: $favoritePath to $newName")
        if (favoriteDao.renameFavorite(favoritePath, newName)) {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.favorite_renamed))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.unable_to_rename_favorite))
        }
    }

    fun refreshStorageItems() {
        _storageItems.value = homeModel.getStorageItems()
    }

    fun getPathForDirectory(): String = _uiState.value?.path ?: ""
}
