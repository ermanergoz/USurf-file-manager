package com.erman.usurf.home.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erman.usurf.R
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.FavoriteKind
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoriesSource
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.logi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val homeModel: HomeModel,
    private val storageAccessPolicy: StorageAccessPolicy,
    private val favoriteRepository: FavoriteRepository,
    private val storageDirectoriesSource: StorageDirectoriesSource,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private val _uiEvents = MutableLiveData<Event<HomeUiEvent>>()
    val uiEvents: LiveData<Event<HomeUiEvent>> = _uiEvents

    val favorites: LiveData<List<FavoriteItem>> = favoriteRepository.getFavorites()

    init {
        refreshStorageItems()
    }

    private fun updateState(transform: (HomeUiState) -> HomeUiState) {
        _uiState.value = transform(_uiState.value ?: HomeUiState())
    }

    fun onStorageButtonClick(path: String) {
        _uiEvents.value = Event(HomeUiEvent.NavigateToDirectory(path))
        if (storageAccessPolicy.needsFirstSafGrant(path)) {
            _uiEvents.value = Event(HomeUiEvent.RequestSafGrant)
        }
    }

    /** Whether a long press on the storage card for [path] offers to grant (or grant again) its SD card access. */
    fun canOfferSafGrant(path: String): Boolean = storageAccessPolicy.canGrantSafAccess(path)

    fun getUsedStoragePercentage(path: String): Int = homeModel.getUsedStoragePercentage(path)

    fun onFavoriteAdd(path: String, name: String): Boolean {
        logi("Add favorite: $name")
        return favoriteRepository.addFavorite(path, name)
    }

    // The repository read what the path holds when it listed the favourites, so this needs no file system call.
    fun onFavoriteClick(item: FavoriteItem) {
        _uiEvents.value =
            Event(
                when (item.kind) {
                    FavoriteKind.FOLDER -> HomeUiEvent.NavigateToDirectory(item.path)
                    FavoriteKind.FILE -> HomeUiEvent.OpenFile(item.path)
                    FavoriteKind.MISSING -> HomeUiEvent.ShowSnackbar(R.string.invalid_favorite)
                }
            )
    }

    /** Open, chosen in the options of the favourite at [path]: as a tap on it; one no longer listed reads as missing. */
    fun onFavoriteOpenChosen(path: String) {
        val item: FavoriteItem? = favorites.value?.firstOrNull { it.path == path }
        if (item == null) {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.invalid_favorite))
        } else {
            onFavoriteClick(item)
        }
    }

    fun onFavoriteLongClick(favoritePath: String, favoriteName: String): Boolean {
        _uiEvents.value = Event(HomeUiEvent.ShowFavoriteOptions(favoritePath, favoriteName))
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

    fun onRenameFavoriteOkPressed(favoritePath: String, newName: String) {
        logi("Rename favorite: $favoritePath to $newName")
        if (favoriteRepository.renameFavorite(favoritePath, newName)) {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.favorite_renamed))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowSnackbar(R.string.unable_to_rename_favorite))
        }
    }

    // Looks for the volumes again, so a card put in while the app was away shows when the screen is back.
    fun refreshStorageItems() {
        viewModelScope.launch {
            val items =
                withContext(ioDispatcher) {
                    homeModel.getStorageItems(storageDirectoriesSource.refreshStorageDirectories())
                }
            if (items != _uiState.value?.storageItems) updateState { it.copy(storageItems = items) }
        }
    }
}
