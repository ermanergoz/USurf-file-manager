package com.erman.usurf.directory.ui

import android.content.Intent
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.FileInformationDialog
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.utils.Event
import com.erman.usurf.directory.model.FileModel
import java.io.File
import java.lang.Exception

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel() {
    private var multiSelectionMode: Boolean = false

    private val _path = MutableLiveData<String>()
    val path: LiveData<String> = _path

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    private val _newActivity = MutableLiveData<Event<Intent?>>()
    val newActivity: LiveData<Event<Intent?>> = _newActivity

    private val _dialog = MutableLiveData<Event<DialogFragment>>()
    val dialog: LiveData<Event<DialogFragment>> = _dialog

    private val _optionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val optionMode: LiveData<Boolean> = _optionMode

    private val _multipleSelection = MutableLiveData<MutableList<FileModel>>().apply {
        value = mutableListOf()
    }
    val multipleSelection: LiveData<MutableList<FileModel>> = _multipleSelection

    private val _moreOptionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val moreOptionMode: LiveData<Boolean> = _moreOptionMode

    fun onFileClick(file: FileModel) {
        if (multiSelectionMode)
            _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
        else {
            if (file.isDirectory)
                _path.value = file.path
            else {
                val intent = directoryModel.openFile(file)
                intent?.let {
                    _newActivity.value = Event(it)
                } ?: let {
                    _toastMessage.value = Event(R.string.unsupported_file)
                }
            }
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        _multipleSelection.value = directoryModel.manageMultipleSelectionList(file, multipleSelection.value!!)
        _optionMode.value = true
        multiSelectionMode = true
        return true
    }

    fun onBackPressed(): Boolean {
        try {
            if (optionMode.value!!) {
                _multipleSelection.value = directoryModel.clearMultipleSelection(multipleSelection.value!!)
                _optionMode.value = false
                _moreOptionMode.value = false
                multiSelectionMode = false
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
        for (i in multipleSelection.value!!.indices) {
            val queueIndicator = ((multipleSelection.value!!.size - (i + 1)) + 1).toString() +
                    " / " + multipleSelection.value!!.size
            _dialog.value = Event(FileInformationDialog(multipleSelection.value!![i], queueIndicator))
        }
    }

    fun onShareClicked() {
        _newActivity.value = Event(directoryModel.share(multipleSelection.value!!))
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

    fun onRenameClicked() {
        Log.e("rename", "clicked")
    }

    fun onDeleteClicked() {
        Log.e("delete", "clicked")
    }
}