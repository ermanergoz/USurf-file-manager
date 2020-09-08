package com.erman.usurf.directory.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.directory.model.*
import com.erman.usurf.utils.Event
import java.io.File
import java.lang.Exception

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel() {
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

    private val _onInformation = MutableLiveData<Event<UIEventArgs.InformationDialogArgs>>()
    val onInformation: LiveData<Event<UIEventArgs.InformationDialogArgs>> = _onInformation

    private val _optionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val optionMode: LiveData<Boolean> = _optionMode

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
        if (multiSelectionMode)
            _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
        else {
            if (file.isDirectory) _path.value = file.path
            else _openFile.value = Event(UIEventArgs.OpenFileActivityArgs(file.path))
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
        _optionMode.value = true
        multiSelectionMode = true
        return true
    }

    private fun turnOffOptionMode() {
        _multipleSelection.value = directoryModel.clearMultipleSelection(multipleSelection.value!!)
        _optionMode.value = false
        _moreOptionMode.value = false
        multiSelectionMode = false
    }

    fun onBackPressed(): Boolean {
        try {
            if (optionMode.value!!) {
                turnOffOptionMode()
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
        if (!path.value.isNullOrEmpty()) {
            try {
                return directoryModel.getFileModelsFromFiles(path.value!!)
            } catch (err: IllegalStateException) {
                _toastMessage.value = Event(R.string.unable_to_open_directory)
                err.printStackTrace()
            }
        }
        return emptyList()
    }

    fun onCompressClicked() {
        Log.e("compress", "clicked")
    }

    fun onExtractClicked() {
        Log.e("extract", "clicked")
    }

    fun onInformationClicked() {
        for (file in multipleSelection.value!!) {
            _onInformation.value = Event(UIEventArgs.InformationDialogArgs(file))
        }
    }

    fun onShareClicked() {
        _onShare.value = Event(UIEventArgs.ShareActivityArgs(multipleSelection.value!!))
    }

    fun onMoreClicked() {
        _moreOptionMode.value = !_moreOptionMode.value!!
    }

    fun onCopyClicked() {
        Log.e("copy", "clicked")
    }

    fun onMoveClicked() {
        Log.e("move", "clicked")
    }

    fun onRenameOkPressed(fileName: String) {
        directoryModel.rename(multipleSelection.value!!, fileName)
        _updateDirectoryList.value = directoryModel.getFileModelsFromFiles(path.value!!)
        turnOffOptionMode()
    }

    fun onRenameClicked() {
        val file: FileModel? = if (multipleSelection.value!!.size == 1) multipleSelection.value!!.first()
        else null
        _onRename.value = Event(UIEventArgs.RenameDialogArgs(file?.name))
    }

    fun onDeleteClicked() {
        Log.e("delete", "clicked")
    }
}