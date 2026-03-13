package com.erman.usurf.directory.ui

import android.content.ActivityNotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
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
                try {
                    directoryModel.openFile(file)
                } catch (err: ActivityNotFoundException) {
                    _toastMessage.value = Event(R.string.file_unsupported_or_no_application)
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

    fun onMoreClicked() {
        _moreOptionMode.value = !_moreOptionMode.value!!
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
        return if (!path.value.isNullOrEmpty()) {
            directoryModel.getFileModelsFromFiles(path.value!!)
        } else emptyList()
    }
}