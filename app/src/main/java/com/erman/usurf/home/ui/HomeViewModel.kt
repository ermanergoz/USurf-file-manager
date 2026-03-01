package com.erman.usurf.home.ui

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.domain.HomePreferencesRepository
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.EXTERNAL_SD_STORAGE_INDEX
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.SINGLE_STORAGE_COUNT
import com.erman.usurf.utils.logi
import java.io.File

class HomeViewModel(
    private val homeModel: HomeModel,
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val homePreferencesRepository: HomePreferencesRepository,
    private val favoriteRepository: FavoriteRepository,
    private val storagePathsProvider: StoragePathsProvider,
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

    val favorites: LiveData<List<FavoriteItem>> = favoriteRepository.getFavorites()

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
        val storageDirectories = storagePathsProvider.getStorageDirectories()
        val isKitkatRemovableStorage = (
            Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT &&
                storageDirectories.size > SINGLE_STORAGE_COUNT &&
                newPath == storageDirectories.elementAt(EXTERNAL_SD_STORAGE_INDEX)
        )
        val showWarning =
            isKitkatRemovableStorage &&
                !homePreferencesRepository.getIsKitkatRemovableStorageWarningDisplayed()
        if (showWarning) {
            _uiEvents.value =
                Event(
                    HomeUiEvent.ShowDialog(
                        DialogArgs.KitkatRemovableStorageDialogArgs(isKitkatRemovableStorage),
                    ),
                )
            homePreferencesRepository.setKitkatRemovableStorageWarningDisplayed(true)
        }
    }

    private fun shouldShowSafDialog(path: String): Boolean {
        if (path == ROOT_DIRECTORY) return false
        if (File(path).canWrite()) return false
        return when {
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> false
            storageDirectoryRepository.getChosenUri() != "" -> false
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
        if (favoriteRepository.addFavorite(path, name)) {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.favorite_created))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.unable_to_create_favorite))
        }
    }

    fun onFavoriteClick(favoritePath: String) {
        if (File(favoritePath).exists()) {
            if (File(favoritePath).isDirectory) {
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
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.invalid_favorite))
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
        if (favoriteRepository.removeFavorite(favoritePath)) {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.favorite_deleted))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.unable_to_delete_favorite))
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
        if (favoriteRepository.renameFavorite(favoritePath, newName)) {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.favorite_renamed))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.unable_to_rename_favorite))
        }
    }

    fun refreshStorageItems() {
        _storageItems.value = homeModel.getStorageItems()
    }

    fun getPathForDirectory(): String = _uiState.value?.path ?: ""
}
