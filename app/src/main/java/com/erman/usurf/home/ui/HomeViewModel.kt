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
import com.erman.usurf.dialog.model.UIEventArgs
import com.erman.usurf.home.data.Shortcut
import com.erman.usurf.home.data.ShortcutDao
import com.erman.usurf.utils.DirectoryPreferenceProvider
import com.erman.usurf.home.model.HomeModel
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

    private val _saf = MutableLiveData<Event<UIEventArgs.SAFActivityArgs>>()
    val saf: MutableLiveData<Event<UIEventArgs.SAFActivityArgs>> = _saf

    private val _onShortcutOption = MutableLiveData<Event<UIEventArgs.ShortcutOptionsDialogArgs>>()
    val onShortcutOption: LiveData<Event<UIEventArgs.ShortcutOptionsDialogArgs>> = _onShortcutOption

    private val _onRename = MutableLiveData<Event<UIEventArgs.RenameDialogArgs>>()
    val onRename: LiveData<Event<UIEventArgs.RenameDialogArgs>> = _onRename

    private val _isRenameMode = MutableLiveData<Boolean>()
    val isRenameMode: LiveData<Boolean> = _isRenameMode

    private val _openFile = MutableLiveData<Event<UIEventArgs.OpenFileActivityArgs>>()
    val openFile: LiveData<Event<UIEventArgs.OpenFileActivityArgs>> = _openFile

    fun onStorageButtonClick(view: View) {
        _path.value = view.tag.toString()
        _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
        path.value?.let { path ->
            if (!File(path).canWrite() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                _saf.value = Event(UIEventArgs.SAFActivityArgs)
        }
    }

    fun getUsedStoragePercentage(view: View): Int {
        return homeModel.getUsedStoragePercentage(view.tag.toString())
    }

    fun saveDocumentTree(treeUri: String) {
        DirectoryPreferenceProvider().editChosenUri(treeUri)
    }

    fun onShortcutAdd(path: String, name: String) {
        logi("Add shortcut: $name")
        shortcutDao.addShortcut(path, name)
    }

    fun onShortcutClick(view: View) {
        val shortcutPath = view.tag.toString()

        if (File(shortcutPath).isDirectory) {
            logd("Open a shortcut directory")
            _path.value = shortcutPath
            _navigateToDirectory.value = Event(R.id.global_action_nav_directory)
        } else _openFile.value = Event(UIEventArgs.OpenFileActivityArgs(shortcutPath))
    }

    fun onShortcutLongClick(view: TextView): Boolean {
        _onShortcutOption.value = Event(UIEventArgs.ShortcutOptionsDialogArgs(view))
        return true
    }

    fun deleteShortcut(shortcutView: TextView) {
        logi("Delete shortcut: " + shortcutView.text)
        shortcutDao.removeShortcut(shortcutView)
    }

    fun renameShortcut(oldName: String) {
        _isRenameMode.value = true
        _onRename.value = Event(UIEventArgs.RenameDialogArgs(oldName))
    }

    fun turnOffRenameMode() {
        _isRenameMode.value = false
    }

    fun onRenameShortcutOkPressed(shortcutView: TextView, shortcutName: String) {
        logi("Rename shortcut: " + shortcutView.text + " to " + shortcutName)
        shortcutDao.renameShortcut(shortcutView, shortcutName)
    }

    fun createStorageButtons() {
        _storageButtons.value = homeModel.createStorageButtons()
    }
}