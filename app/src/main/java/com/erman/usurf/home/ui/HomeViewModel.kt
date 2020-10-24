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
import com.erman.usurf.home.data.Shortcut
import com.erman.usurf.home.data.ShortcutDao
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.model.StorageAccessArgs
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.logi
import io.realm.Realm
import java.io.File

class HomeViewModel(private val homeModel: HomeModel) : ViewModel() {
    private var realm = Realm.getDefaultInstance()
    private var shortcutDao = ShortcutDao(realm)

    private val _storageButtons = MutableLiveData<MutableList<StorageButtonBinding>>().apply {
        value = homeModel.createStorageButtons()
    }
    val storageButtons: LiveData<MutableList<StorageButtonBinding>> = _storageButtons

    var shortcuts: LiveData<List<Shortcut>> = Transformations.map(shortcutDao.getShortcuts()) { realmResult ->
        realm.copyFromRealm(realmResult)
    }

    private val _navigateToDirectory = MutableLiveData<Event<Int>>()
    val navigateToDirectory: MutableLiveData<Event<Int>> = _navigateToDirectory

    private val _path = MutableLiveData<String>()
    val path: MutableLiveData<String> = _path

    private val _saf = MutableLiveData<Event<StorageAccessArgs.SAFActivityArgs>>()
    val saf: MutableLiveData<Event<StorageAccessArgs.SAFActivityArgs>> = _saf

    private val _onShortcutOption = MutableLiveData<Event<DialogArgs.ShortcutOptionsDialogArgs>>()
    val onShortcutOption: LiveData<Event<DialogArgs.ShortcutOptionsDialogArgs>> = _onShortcutOption

    private val _onRename = MutableLiveData<Event<DialogArgs.RenameDialogArgs>>()
    val onRename: LiveData<Event<DialogArgs.RenameDialogArgs>> = _onRename

    private val _isRenameMode = MutableLiveData<Boolean>()
    val isRenameMode: LiveData<Boolean> = _isRenameMode

    private val _openFile = MutableLiveData<Event<DialogArgs.OpenFileActivityArgs>>()
    val openFile: LiveData<Event<DialogArgs.OpenFileActivityArgs>> = _openFile

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    fun onStorageButtonClick(view: View) {
        _path.value = view.tag.toString()
        _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
        path.value?.let { path ->
            if (!File(path).canWrite() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && StorageDirectoryPreferenceProvider().getChosenUri() == "")
                _saf.value = Event(StorageAccessArgs.SAFActivityArgs)
        }
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }

    fun onShortcutAdd(path: String, name: String) {
        logi("Add shortcut: $name")
        if (shortcutDao.addShortcut(path, name))
            _toastMessage.value = Event(R.string.shortcut_created) //TODO: toast massege doesnt get displayed
        else
            _toastMessage.value = Event(R.string.unable_to_create_shortcut)
    }

    fun onShortcutClick(view: View) {
        val shortcutPath = view.tag.toString()

        if (File(shortcutPath).isDirectory) {
            logd("Open a shortcut directory")
            _path.value = shortcutPath
            _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
        } else _openFile.value = Event(DialogArgs.OpenFileActivityArgs(shortcutPath))
    }

    fun onShortcutLongClick(view: TextView): Boolean {
        _onShortcutOption.value = Event(DialogArgs.ShortcutOptionsDialogArgs(view))
        return true
    }

    fun deleteShortcut(shortcutView: TextView) {
        logi("Delete shortcut: " + shortcutView.text)
        if (shortcutDao.removeShortcut(shortcutView))
            _toastMessage.value = Event(R.string.shortcut_deleted) //TODO: toast massege doesnt get displayed
        else
            _toastMessage.value = Event(R.string.unable_to_delete_shortcut)
    }

    fun renameShortcut(oldName: String) {
        _isRenameMode.value = true
        _onRename.value = Event(DialogArgs.RenameDialogArgs(oldName))
    }

    fun turnOffRenameMode() {
        _isRenameMode.value = false
    }

    fun onRenameShortcutOkPressed(shortcutView: TextView, shortcutName: String) {
        logi("Rename shortcut: " + shortcutView.text + " to " + shortcutName)
        if (shortcutDao.renameShortcut(shortcutView, shortcutName))
            _toastMessage.value = Event(R.string.shortcut_renamed) //TODO: toast massege doesnt get displayed
        else
            _toastMessage.value = Event(R.string.unable_to_rename_shortcut)
    }

    fun createStorageButtons() {
        _storageButtons.value = homeModel.createStorageButtons()
    }
}