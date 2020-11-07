package com.erman.usurf.directory.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.*
import com.erman.usurf.preference.data.PreferenceProvider
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

    private val _openFile = MutableLiveData<Event<DialogArgs.OpenFileActivityArgs>>()
    val openFile: LiveData<Event<DialogArgs.OpenFileActivityArgs>> = _openFile

    private val _onShare = MutableLiveData<Event<DialogArgs.ShareActivityArgs>>()
    val onShare: LiveData<Event<DialogArgs.ShareActivityArgs>> = _onShare

    private val _onRename = MutableLiveData<Event<DialogArgs.RenameDialogArgs>>()
    val onRename: LiveData<Event<DialogArgs.RenameDialogArgs>> = _onRename

    private val _onCompress = MutableLiveData<Event<DialogArgs.CompressDialogArgs>>()
    val onCompress: LiveData<Event<DialogArgs.CompressDialogArgs>> = _onCompress

    private val _onCreateFile = MutableLiveData<Event<DialogArgs.CreateFileDialogArgs>>()
    val onCreateFile: LiveData<Event<DialogArgs.CreateFileDialogArgs>> = _onCreateFile

    private val _onCreateFolder = MutableLiveData<Event<DialogArgs.CreateFolderDialogArgs>>()
    val onCreateFolder: LiveData<Event<DialogArgs.CreateFolderDialogArgs>> = _onCreateFolder

    private val _onInformation = MutableLiveData<Event<DialogArgs.InformationDialogArgs>>()
    val onInformation: LiveData<Event<DialogArgs.InformationDialogArgs>> = _onInformation

    private val _onAddShortcut = MutableLiveData<Event<DialogArgs.ShortcutDialogArgs>>()
    val onAddShortcut: LiveData<Event<DialogArgs.ShortcutDialogArgs>> = _onAddShortcut

    private val _isSingleOperationMode = MutableLiveData<Boolean>()
    val isSingleOperationMode: LiveData<Boolean> = _isSingleOperationMode

    private val _fileSearchQuery = MutableLiveData<String>()
    val fileSearchQuery: LiveData<String> = _fileSearchQuery

    private val _loading = MutableLiveData<Boolean>().apply {
        value = false
    }
    val loading: LiveData<Boolean> = _loading

    private val _showLoadingMessage = MutableLiveData<Boolean>().apply {
        value = false
    }
    val showLoadingMessage: LiveData<Boolean> = _showLoadingMessage

    private val _fileSearchMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val fileSearchMode: LiveData<Boolean> = _fileSearchMode

    private val _onFileSearch = MutableLiveData<Event<DialogArgs.FileSearchDialogArgs>>()
    val onFileSearch: LiveData<Event<DialogArgs.FileSearchDialogArgs>> = _onFileSearch

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

    private val _menuMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val menuMode: LiveData<Boolean> = _menuMode

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

    private val _isRootMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isRootMode: LiveData<Boolean> = _isRootMode

    fun onFileClick(file: FileModel) {
        if (multiSelectionMode) {
            _isSingleOperationMode.value = false
            multipleSelection.value?.let { multipleSelection ->
                _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection)
                if (multipleSelection.size == 1)
                    _isSingleOperationMode.value = true
                else if (multipleSelection.size == 0) {
                    turnOffOptionPanel()
                    clearMultipleSelection()
                }
            }
        } else {
            _fileSearchMode.value = false
            if (file.isDirectory) _path.value = file.path
            else _openFile.value = Event(DialogArgs.OpenFileActivityArgs(file.path))
        }
        _isRootMode.value = file.isInRoot
    }

    fun onFileLongClick(file: FileModel): Boolean {
        multipleSelection.value?.let { multipleSelection ->
            _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection)
        }
        _isRootMode.value = file.isInRoot
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
        _menuMode.value = false
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
            if (optionMode.value!! && !copyMode.value!! && !moveMode.value!! || menuMode.value!!) {
                turnOffOptionPanel()
                clearMultipleSelection()
                return true
            } else {
                _fileSearchMode.value = false
                path.value?.let { path ->
                    File(path).parent?.let { parent ->
                        val prevFile = File(parent)
                        if ((prevFile.canRead() && prevFile.path != "/") || PreferenceProvider().getRootAccessPreference()) {
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

    fun setPath(path: String?) {
        _path.value = path
        _fileSearchQuery.value = null
    }

    fun getFileList() {
        if (!path.value.isNullOrEmpty()) {
            try {
                _loading.value = true
                launch {
                    path.value?.let { path ->
                        val directory = directoryModel.getFileModelsFromFiles(path)
                        if (directory.isNullOrEmpty())
                            _toastMessage.value = Event(R.string.empty_folder)
                        _updateDirectoryList.value = directory
                    }
                    _loading.value = false
                }
            } catch (err: IllegalStateException) {
                _toastMessage.value = Event(R.string.unable_to_open_directory)
                loge("getFileList $err")
            }
        } else {
            _loading.value = true
            _showLoadingMessage.value = true
            launch {
                _updateDirectoryList.value = directoryModel.getFilesToClean()
                _loading.value = false
                _showLoadingMessage.value = false
            }
        }
    }

    fun getSearchedFiles() {
        fileSearchQuery.value?.let {
            _loading.value = true
            _showLoadingMessage.value = true
            launch {
                val fileList = directoryModel.getSearchedDeviceFiles(it)
                if (fileList.isNullOrEmpty())
                    _toastMessage.value = Event(R.string.empty_folder)
                _updateDirectoryList.value = fileList
                _menuMode.value = false
                _loading.value = false
                _showLoadingMessage.value = false
            }
        }
    }

    fun compress() {
        _onCompress.value = Event(DialogArgs.CompressDialogArgs)
    }

    private fun refreshFileList() {
        launch {
            fileSearchMode.value?.let { isFileSearchMode ->
                if (isFileSearchMode) {
                    fileSearchQuery.value?.let { fileSearchQuery ->
                        _updateDirectoryList.value = directoryModel.getSearchedDeviceFiles(fileSearchQuery)
                    }
                } else {
                    path.value?.let { path ->
                        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path)
                    }
                }
            }
        }
    }

    fun onFileCompressOkPressed(zipNameWithExtension: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.compressing)
        logd("onFileCompressOkPressed")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    directoryModel.compressFiles(multipleSelection, zipNameWithExtension)
                    refreshFileList()
                    _toastMessage.value = Event(R.string.compressing_successful)
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
        multipleSelection.value?.let { multipleSelection ->
            if (multipleSelection.first().extension == "zip") {
                _toastMessage.value = Event(R.string.extracting)
                logd("extract")
                launch {
                    try {
                        directoryModel.extractFiles(multipleSelection.first())
                        refreshFileList()
                        _toastMessage.value = Event(R.string.extracting_successful)

                    } catch (err: CancellationException) {
                        _toastMessage.value = Event(R.string.error_while_extracting)
                        loge("extract $err")
                    } finally {
                        clearMultipleSelection()
                    }
                }
            } else {
                _toastMessage.value = Event(R.string.invalid_extension)
                clearMultipleSelection()
            }
        }
    }

    fun showFileInformation() {
        multipleSelection.value?.let { multipleSelection ->
            for (file in multipleSelection) {
                _onInformation.value = Event(DialogArgs.InformationDialogArgs(file))
            }
        }
    }

    fun share() {
        multipleSelection.value?.let { multipleSelection ->
            _onShare.value = Event(DialogArgs.ShareActivityArgs(multipleSelection))
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
                        path.value?.let { path ->
                            multipleSelection.value?.let { multipleSelection ->
                                directoryModel.copyFile(multipleSelection, path)
                                refreshFileList()
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
                                refreshFileList()
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
                    directoryModel.rename(multipleSelection.last(), fileName)
                    refreshFileList()
                    _toastMessage.value = Event(R.string.renaming_successful)
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
            _onRename.value = Event(DialogArgs.RenameDialogArgs(multipleSelection.last().name))
        }
    }

    fun delete() {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.deleting)
        logd("delete")
        launch {
            try {
                multipleSelection.value?.let { multipleSelection ->
                    directoryModel.delete(multipleSelection)
                    refreshFileList()
                    _toastMessage.value = Event(R.string.deleting_successful)
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
        menuMode.value?.let {
            _menuMode.value = !it
        }
    }

    fun createFolder() {
        _onCreateFolder.value = Event(DialogArgs.CreateFolderDialogArgs)
    }

    fun createFile() {
        _onCreateFile.value = Event(DialogArgs.CreateFileDialogArgs)
    }

    fun onFolderCreateOkPressed(folderName: String) {
        turnOffOptionPanel()
        _toastMessage.value = Event(R.string.creating)
        logd("onFolderCreateOkPressed")
        launch {
            try {
                path.value?.let { path ->
                    directoryModel.createFolder(path, folderName)
                    refreshFileList()
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
                    refreshFileList()
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

    fun onFileSearchOkPressed(searchQuery: String) {
        _fileSearchMode.value = true
        _fileSearchQuery.value = searchQuery
    }

    fun deviceWideSearch() {
        _onFileSearch.value = Event(DialogArgs.FileSearchDialogArgs)
    }

    fun shortcutButton() {
        logd("shortcutButton")
        multipleSelection.value?.let {
            val file = it.last()
            _onAddShortcut.value = Event(DialogArgs.ShortcutDialogArgs(file.path))
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job = Job()
}