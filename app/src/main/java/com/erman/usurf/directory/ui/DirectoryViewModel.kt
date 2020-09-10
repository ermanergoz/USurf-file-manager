package com.erman.usurf.directory.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.UIEventArgs
import com.erman.usurf.directory.model.*
import com.erman.usurf.utils.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

    private val _isOperationInProgress = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isOperationInProgress: LiveData<Boolean> = _isOperationInProgress

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

    fun onFileClick(file: FileModel) {
        _isOperationInProgress.value = true
        if (multiSelectionMode)
            _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
        else {
            if (file.isDirectory) _path.value = file.path
            else _openFile.value = Event(UIEventArgs.OpenFileActivityArgs(file.path))
        }
        _isOperationInProgress.value = false
    }

    fun onFileLongClick(file: FileModel): Boolean {
        _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
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
        _multipleSelection.value = directoryModel.clearMultipleSelection(multipleSelection.value!!)
    }

    fun onBackPressed(): Boolean {
        try {
            if (optionMode.value!!) {
                turnOffOptionPanel()
                clearMultipleSelection()
                return true
            } else {
                val prevFile = File(File(path.value!!).parent!!)
                if (prevFile.canRead()) {
                    _path.value = prevFile.path
                    return true
                }
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }
        return false
    }

    fun setPath(path: String) {
        _path.value = path
    }

    fun getFileList(): List<FileModel> {
        _isOperationInProgress.value = true
        if (!path.value.isNullOrEmpty()) {
            try {
                val fileList = directoryModel.getFileModelsFromFiles(path.value!!)
                _isOperationInProgress.value = false
                return fileList
            } catch (err: IllegalStateException) {
                _toastMessage.value = Event(R.string.unable_to_open_directory)
                err.printStackTrace()
            }
        }
        _isOperationInProgress.value = false
        return emptyList()
    }

    fun compress() {
        _onCompress.value = Event(UIEventArgs.CompressDialogArgs)
    }

    fun onFileCompressOkPressed(name: String) {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.compressFile(multipleSelection.value!!, name) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.compressing_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_while_compressing)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    fun extract() {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.extractFiles(multipleSelection.value!!) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.extracting_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_while_extracting)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    fun showFileInformation() {
        for (file in multipleSelection.value!!) {
            _onInformation.value = Event(UIEventArgs.InformationDialogArgs(file))
        }
    }

    fun share() {
        _onShare.value = Event(UIEventArgs.ShareActivityArgs(multipleSelection.value!!))
    }

    fun showMoreOption() {
        _moreOptionMode.value = !_moreOptionMode.value!!
    }

    fun copy() {
        _copyMode.value = true
        multiSelectionMode = false
    }

    fun move() {
        _moveMode.value = true
        multiSelectionMode = false
    }

    fun confirmAction() {
        when {
            copyMode.value!! -> {
                turnOffOptionPanel()
                _isOperationInProgress.value = true
                launch {
                    when {
                        directoryModel.copyFile(multipleSelection.value!!, path.value!!) -> {
                            _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                            _toastMessage.value = Event(R.string.copy_successful)
                        }
                        directoryModel.copyFileWithSaf(multipleSelection.value!!, path.value!!) -> {
                            _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                            _toastMessage.value = Event(R.string.copy_successful)
                        }
                        else -> _toastMessage.value = Event(R.string.error_while_copying)
                    }
                    _isOperationInProgress.value = false
                    clearMultipleSelection()
                }
            }
            moveMode.value!! -> {
                turnOffOptionPanel()
                _isOperationInProgress.value = true
                launch {
                    when {
                        directoryModel.moveFile(multipleSelection.value!!, path.value!!) -> {
                            _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                            _toastMessage.value = Event(R.string.moving_successful)
                        }
                        directoryModel.moveFileWithSaf(multipleSelection.value!!, path.value!!) -> {
                            _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                            _toastMessage.value = Event(R.string.moving_successful)
                        }
                        else -> _toastMessage.value = Event(R.string.error_while_moving)
                    }
                    _isOperationInProgress.value = false
                    clearMultipleSelection()
                }
            }
        }
    }


    fun onRenameOkPressed(fileName: String) {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.rename(multipleSelection.value!!, fileName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.renaming_successful)
                }
                directoryModel.renameWithSaf(multipleSelection.value!!, fileName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.renaming_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_while_renaming)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    fun rename() {
        val file: FileModel? = if (multipleSelection.value!!.size == 1) multipleSelection.value!!.first()
        else null
        _onRename.value = Event(UIEventArgs.RenameDialogArgs(file?.name))
    }

    fun delete() {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.delete(multipleSelection.value!!) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.deleting_successful)
                }
                directoryModel.deleteWithSaf(multipleSelection.value!!) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.deleting_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_while_deleting)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    fun onCreate() {
        _createMode.value = !createMode.value!!
    }

    fun createFolder() {
        _onCreateFolder.value = Event(UIEventArgs.CreateFolderDialogArgs)
    }

    fun createFile() {
        _onCreateFile.value = Event(UIEventArgs.CreateFileDialogArgs)
    }

    fun onFolderCreateOkPressed(folderName: String) {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.createFolder(path.value!!, folderName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.folder_creation_successful)
                }
                directoryModel.createFolderWithSaf(path.value!!, folderName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.folder_creation_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_when_creating_folder)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    fun onFileCreateOkPressed(fileName: String) {
        turnOffOptionPanel()
        _isOperationInProgress.value = true
        launch {
            when {
                directoryModel.createFile(path.value!!, fileName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.file_creation_successful)
                }
                directoryModel.createFileWithSaf(path.value!!, fileName) -> {
                    _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
                    _toastMessage.value = Event(R.string.file_creation_successful)
                }
                else -> _toastMessage.value = Event(R.string.error_when_creating_file)
            }
            _isOperationInProgress.value = false
            clearMultipleSelection()
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job = Job()
}