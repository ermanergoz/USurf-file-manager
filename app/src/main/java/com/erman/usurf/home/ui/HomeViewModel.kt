package com.erman.usurf.home.ui

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.erman.usurf.R
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.home.data.Favorite
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.home.data.HomePreferenceProvider
import com.erman.usurf.home.model.HomeModel
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

    private val _storageButtons =
        MutableLiveData<MutableList<StorageButtonBinding>>().apply {
            value = homeModel.createStorageButtons()
        }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    var favorites: LiveData<List<Favorite>> =
        favoriteDao.getFavorites().map { realmResults ->
            realm.copyFromRealm(realmResults)
        }

    private fun updateState(transform: (HomeUiState) -> HomeUiState) {
        _uiState.value = transform(_uiState.value ?: HomeUiState())
    }

    fun onStorageButtonClick(view: View) {
        val newPath = view.tag.toString()
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
        if (isKitkatRemovableStorage && !homePreferenceProvider.getIsKitkatRemovableStorageWarningDisplayedPreference()) {
            _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.KitkatRemovableStorageDialogArgs(isKitkatRemovableStorage)))
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

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
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

    fun onFavoriteClick(view: View) {
        val favoritePath = view.tag.toString()
        if (File(favoritePath).exists()) {
            if (File(favoritePath).isDirectory) {
                logd("Open a favorite directory")
                updateState { it.copy(path = favoritePath) }
                _uiEvents.value = Event(HomeUiEvent.NavigateToDirectory(R.id.global_action_nav_directory, favoritePath))
            } else {
                _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.OpenFileActivityArgs(favoritePath)))
            }
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.invalid_favorite))
        }
    }

    fun onFavoriteLongClick(view: TextView): Boolean {
        _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.FavoriteOptionsDialogArgs(view)))
        return true
    }

    fun deleteFavorites(favoriteView: TextView) {
        logi("Delete favorite: ${favoriteView.text}")
        if (favoriteDao.removeFavorite(favoriteView)) {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.favorite_deleted))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.unable_to_delete_favorite))
        }
    }

    fun renameFavorite(oldName: String) {
        updateState { it.copy(isRenameMode = true) }
        _uiEvents.value = Event(HomeUiEvent.ShowDialog(DialogArgs.RenameDialogArgs(oldName)))
    }

    fun turnOffRenameMode() {
        updateState { it.copy(isRenameMode = false) }
    }

    fun onRenameFavoriteOkPressed(
        favoriteView: TextView,
        favoriteName: String,
    ) {
        logi("Rename favorite: ${favoriteView.text} to $favoriteName")
        if (favoriteDao.renameFavorite(favoriteView, favoriteName)) {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.favorite_renamed))
        } else {
            _uiEvents.value = Event(HomeUiEvent.ShowToast(R.string.unable_to_rename_favorite))
        }
    }

    fun createStorageButtons() {
        _storageButtons.value = homeModel.createStorageButtons()
    }

    fun getPathForDirectory(): String = _uiState.value?.path ?: ""
}
