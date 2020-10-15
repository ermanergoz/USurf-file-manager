package com.erman.usurf.directory.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.UIEventArgs
import com.erman.usurf.directory.model.*
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel(), CoroutineScope {
    private var multiSelectionMode: Boolean = false

    private val _path = MutableLiveData<String>()
    val path: LiveData<String> = _path

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    private val _openFile = MutableLiveData<Event<UIEventArgs.OpenFileActivityArgs>>()
    val openFile: LiveData<Event<UIEventArgs.OpenFileActivityArgs>> = _openFile

    private val _onShare = MutableLiveData<Event<UIEventArgs.ShareActivityArgs>>()
    val onShare: LiveData<Event<UIEventArgs.ShareActivityArgs>> = _onShare

    private val _onRename = MutableLiveData<Event<UIEventArgs.RenameDialogArgs>>()
    val onRename: LiveData<Event<UIEventArgs.RenameDialogArgs>> = _onRename

    private val _onCompress = MutableLiveData<Event<UIEventArgs.CompressDialogArgs>>()
    val onCompress: LiveData<Event<UIEventArgs.CompressDialogArgs>> = _onCompress

    private val _onCreateFile = MutableLiveData<Event<UIEventArgs.CreateFileDialogArgs>>()
    val onCreateFile: LiveData<Event<UIEventArgs.CreateFileDialogArgs>> = _onCreateFile

    private val _onCreateFolder = MutableLiveData<Event<UIEventArgs.CreateFolderDialogArgs>>()
    val onCreateFolder: LiveData<Event<UIEventArgs.CreateFolderDialogArgs>> = _onCreateFolder

    private val _onInformation = MutableLiveData<Event<UIEventArgs.InformationDialogArgs>>()
    val onInformation: LiveData<Event<UIEventArgs.InformationDialogArgs>> = _onInformation

    private val _onAddShortcut = MutableLiveData<Event<UIEventArgs.ShortcutDialogArgs>>()
    val onAddShortcut: LiveData<Event<UIEventArgs.ShortcutDialogArgs>> = _onAddShortcut

    private val _isSingleOperationMode = MutableLiveData<Boolean>()
    val isSingleOperationMode: LiveData<Boolean> = _isSingleOperationMode

    private val _copyMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val copyMode: LiveData<Boolean> = _copyMode

    private val _moveMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val moveMode: LiveData<Boolean> = _moveMode

    private val _optionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val optionMode: LiveData<Boolean> = _optionMode

    private val _createMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val createMode: LiveData<Boolean> = _createMode

    private val _multipleSelection = MutableLiveData<MutableList<FileModel>>().apply {
        value = mutableListOf()
    }
    val multipleSelection: LiveData<MutableList<FileModel>> = _multipleSelection

    private val _updateDirectoryList = MutableLiveData<List<FileModel>>().apply {
        value = mutableListOf()
    }
    val updateDirectoryList: LiveData<List<FileModel>> = _updateDirectoryList

    private val _moreOptionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val moreOptionMode: LiveData<Boolean> = _moreOptionMode

    private val _isEmptyDir = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isEmptyDir: LiveData<Boolean> = _isEmptyDir

    fun getSearchedDeviceFiles(storagePaths: ArrayList<String>, searchQuery: String): List<File> {
        val fileList = mutableListOf<File>()
        try {
            for (i in storagePaths.indices) {
                Log.e("strg", storagePaths[i])
                fileList.addAll(getSubSearchedFiles(File(storagePaths[i]), searchQuery))
            }
            return fileList.filter { file ->
                searchQuery.decapitalize().toRegex().containsMatchIn(file.nameWithoutExtension.decapitalize())
            }
        } catch (err: Exception) {
            Log.e("getSearchedDeviceFiles", err.toString())
        }
        return emptyList()
    }

    fun getSubSearchedFiles(directory: File, searchQuery: String, res: MutableSet<File> = mutableSetOf<File>()): Set<File> {
        //Depth first search algorithm
        for (file in directory.listFiles()!!.toSet()) {
            if (file.isDirectory) {
                getSubSearchedFiles(file, searchQuery, res)
            } else {
                res.add(file)
            }
            res.addAll(directory.listFiles()!!.toSet())
        }
        return res
    }

    fun onFileClick(file: FileModel) {
        if (multiSelectionMode) {
            _isSingleOperationMode.value = false
            multipleSelection.value?.let { multipleSelection ->
                _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection)
                if (multipleSelection.size == 1)
                    _isSingleOperationMode.value = true
            }
        } else {
            if (file.isDirectory) _path.value = file.path
            else _openFile.value = Event(UIEventArgs.OpenFileActivityArgs(file.path))
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        multipleSelection.value?.let { multipleSelection ->
            _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection)
        }
        _isSingleOperationMode.value = true
        _optionMode.value = true
        multiSelectionMode = true
        return true
    }

    fun turnOffOptionPanel() {
        _optionMode.value = false
        _moreOptionMode.value = false
        multiSelectionMode = false
        _copyMode.value = false
        _moveMode.value = false
        _createMode.value = false
    }

    fun clearMultipleSelection() {
        multipleSelection.value?.let { multipleSelection ->
            _multipleSelection.value = directoryModel.clearMultipleSelection(multipleSelection)
        }
    }

    fun endCopyMode() {
        turnOffOptionPanel()
        clearMultipleSelection()
    }

    fun onBackPressed(): Boolean {
        try {
            if (optionMode.value!! && !copyMode.value!! && !moveMode.value!!) {
                turnOffOptionPanel()
                clearMultipleSelection()
                return true
            } else {
                path.value?.let { path ->
                    File(path).parent?.let { parent ->
                        val prevFile = File(parent)
                        if (prevFile.canRead() && prevFile.path != "/") {
                            _path.value = prevFile.path
                            return true
                        }
                    }
                }
            }
        } catch (err: Exception) {
            err.printStackTrace()
            loge("onBackPressed $err")
        }
        return false
    }

    fun setPath(path: String) {
        _path.value = path
    }

    fun getFileList(): List<FileModel> {
        if (!path.value.isNullOrEmpty()) {
            try {
                path.value?.let { path ->
                    val directory = directoryModel.getFileModelsFromFiles(path)
                    _isEmptyDir.value = directory.isNullOrEmpty()
                    return directory
                }
            } catch (err: IllegalStateException) {
                _toastMessage.value = Event(R.string.unable_to_open_directory)
                loge("getFileList $err")
            }
        }
        return emptyList()
    }

    fun compress() {
        _onCompress.value = Event(UIEventArgs.CompressDialogArgs)
    }

    fun onFileCompressOkPressed(name: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.compressing)
        logd("onFileCompressOkPressed")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    directoryModel.compressFile(multipleSelection, name)
                    path.value?.let { path ->
                        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                        _toastMessage.value = Event(R.string.compressing_successful)
                    }
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_while_compressing)
                loge("onFileCompressOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun extract() {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.extracting)
        logd("extract")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    directoryModel.extractFiles(multipleSelection)
                    path.value?.let { path ->
                        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                        _toastMessage.value = Event(R.string.extracting_successful)
                    }
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_while_extracting)
                loge("extract $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun showFileInformation() {
        multipleSelection.value?.let { multipleSelection ->
            for (file in multipleSelection) {
                _onInformation.value = Event(UIEventArgs.InformationDialogArgs(file))
            }
        }
    }

    fun share() {
        multipleSelection.value?.let { multipleSelection ->
            _onShare.value = Event(UIEventArgs.ShareActivityArgs(multipleSelection))
        }
    }

    fun showMoreOption() {
        _moreOptionMode.value?.let {
            _moreOptionMode.value = !it
        }
    }

    fun copy() {
        _copyMode.value = true
        _moreOptionMode.value = false
        multiSelectionMode = false
    }

    fun move() {
        _moveMode.value = true
        _moreOptionMode.value = false
        multiSelectionMode = false
    }

    fun confirmAction() {
        when {
            copyMode.value!! -> {
                _toastMessage.value = Event(R.string.copying)
                logd("copy - copyMode")
                launch {
                    try {
                        multipleSelection.value?.let { multipleSelection ->
                            path.value?.let { path ->
                                directoryModel.copyFile(multipleSelection, path)
                                _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                                _toastMessage.value = Event(R.string.copy_successful)
                            }
                        }
                    } catch (err: CancellationException) {
                        _toastMessage.value = Event(R.string.error_while_copying)
                        loge("confirmAction-copyMode $err")
                    }
                }
            }
            moveMode.value!! -> {
                turnOffOptionPanel()
                _toastMessage.value = Event(R.string.moving)
                logd("move - moveMode")
                launch {
                    try {
                        multipleSelection.value?.let { multipleSelection ->
                            path.value?.let { path ->
                                directoryModel.moveFile(multipleSelection, path)
                                _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                                _toastMessage.value = Event(R.string.moving_successful)
                            }
                        }
                    } catch (err: CancellationException) {
                        _toastMessage.value = Event(R.string.error_while_moving)
                        loge("confirmAction-moveMode $err")
                    } finally {
                        clearMultipleSelection()
                    }
                }
            }
        }
    }

    fun onRenameOkPressed(fileName: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.renaming)
        logd("onRenameOkPressed")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    path.value?.let { path ->
                        directoryModel.rename(multipleSelection.last(), fileName)
                        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                        _toastMessage.value = Event(R.string.renaming_successful)
                    }
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_while_renaming)
                loge("onRenameOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun rename() {
        multipleSelection.value?.let { multipleSelection ->
            _onRename.value = Event(UIEventArgs.RenameDialogArgs(multipleSelection.last().name))
        }
    }

    fun delete() {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.deleting)
        logd("delete")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    path.value?.let { path ->
                        directoryModel.delete(multipleSelection)
                        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                        _toastMessage.value = Event(R.string.deleting_successful)
                    }
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_while_deleting)
                loge("delete $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun onCreate() {
        createMode.value?.let {
            _createMode.value = !it
        }
    }

    fun createFolder() {
        _onCreateFolder.value = Event(UIEventArgs.CreateFolderDialogArgs)
    }

    fun createFile() {
        _onCreateFile.value = Event(UIEventArgs.CreateFileDialogArgs)
    }

    fun onFolderCreateOkPressed(folderName: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.creating)
        logd("onFolderCreateOkPressed")
        launch {
            try {
                path.value?.let { path ->
                    directoryModel.createFolder(path, folderName)
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                    _toastMessage.value = Event(R.string.folder_creation_successful)
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_when_creating_folder)
                loge("onFolderCreateOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun onFileCreateOkPressed(fileName: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.creating)
        logd("onFileCreateOkPressed")
        launch {
            try {
                path.value?.let { path ->
                    directoryModel.createFile(path, fileName)
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                    _toastMessage.value = Event(R.string.file_creation_successful)
                }
            } catch (err: CancellationException) {
                _toastMessage.value = Event(R.string.error_when_creating_file)
                loge("onFileCreateOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun onFileSearchOkPressed(fileName: String) {

    }

    fun shortcutButton() {
        logd("shortcutButton")
        multipleSelection.value?.let {
            val file = it.last()
            _onAddShortcut.value = Event(UIEventArgs.ShortcutDialogArgs(file.path))
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job = Job()
}