package com.erman.usurf.home.ui

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.home.data.Favorite
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.home.data.HomePreferenceProvider
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.logi
import io.realm.Realm
import java.io.File

class HomeViewModel(private val homeModel: HomeModel,
                    private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
                    private val homePreferenceProvider: HomePreferenceProvider,
                    private val realm: Realm,
                    private val favoriteDao: FavoriteDao) : ViewModel() {

    private val _storageButtons = MutableLiveData<MutableList<StorageButtonBinding>>().apply {
        value = homeModel.createStorageButtons()
    }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    var favorites: LiveData<List<Favorite>> = Transformations.map(favoriteDao.getFavorites()) { realmResult ->
        realm.copyFromRealm(realmResult)
    }

    private val _navigateToDirectory = MutableLiveData<Event<Int>>()
    val navigateToDirectory: MutableLiveData<Event<Int>> = _navigateToDirectory

    private val _path = MutableLiveData<String>()
    val path: MutableLiveData<String> = _path

    private val _dialog = MutableLiveData<Event<DialogArgs>>()
    val dialog: LiveData<Event<DialogArgs>> = _dialog

    private val _isRenameMode = MutableLiveData<Boolean>()
    val isRenameMode: LiveData<Boolean> = _isRenameMode

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    fun onStorageButtonClick(view: View) {
        _path.value = view.tag.toString()
        _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
        path.value?.let { path ->
            if (path != ROOT_DIRECTORY && !File(path).canWrite() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && storageDirectoryPreferenceProvider.getChosenUri() == "")
                _dialog.value = Event(DialogArgs.SAFActivityArgs)
        }

        val isKitkatRemovableStorage = (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT &&
                StoragePaths().getStorageDirectories().size > 1 &&
                path.value == StoragePaths().getStorageDirectories().elementAt(1))

        if (isKitkatRemovableStorage && !homePreferenceProvider.getIsKitkatRemovableStorageWarningDisplayedPreference()) {
            _dialog.value = Event(DialogArgs.KitkatRemovableStorageDialogArgs(isKitkatRemovableStorage))
            homePreferenceProvider.editIsKitkatRemovableStorageWarningDisplayedPreference(true)
        }
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }

    fun onFavoriteAdd(path: String, name: String) {
        logi("Add favorite: $name")
        if (favoriteDao.addFavorite(path, name))
            _toastMessage.value = Event(R.string.favorite_created)
        else
            _toastMessage.value = Event(R.string.unable_to_create_favorite)
    }

    fun onFavoriteClick(view: View) {
        val favoritePath = view.tag.toString()

        if (File(favoritePath).exists()) {
            if (File(favoritePath).isDirectory) {
                logd("Open a favorite directory")
                _path.value = favoritePath
                _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
            } else _dialog.value = Event(DialogArgs.OpenFileActivityArgs(favoritePath))
        } else _toastMessage.value = Event(R.string.invalid_favorite)
    }

    fun onFavoriteLongClick(view: TextView): Boolean {
        _dialog.value = Event(DialogArgs.FavoriteOptionsDialogArgs(view))
        return true
    }

    fun deleteFavorites(favoriteView: TextView) {
        logi("Delete favorite: " + favoriteView.text)
        if (favoriteDao.removeFavorite(favoriteView))
            _toastMessage.value = Event(R.string.favorite_deleted)
        else
            _toastMessage.value = Event(R.string.unable_to_delete_favorite)
    }

    fun renameFavorite(oldName: String) {
        _isRenameMode.value = true
        _dialog.value = Event(DialogArgs.RenameDialogArgs(oldName))
    }

    fun turnOffRenameMode() {
        _isRenameMode.value = false
    }

    fun onRenameFavoriteOkPressed(favoriteView: TextView, favoriteName: String) {
        logi("Rename favorite: " + favoriteView.text + " to " + favoriteName)
        if (favoriteDao.renameFavorite(favoriteView, favoriteName))
            _toastMessage.value = Event(R.string.favorite_renamed)
        else
            _toastMessage.value = Event(R.string.unable_to_rename_favorite)
    }

    fun createStorageButtons() {
        _storageButtons.value = homeModel.createStorageButtons()
    }
}
