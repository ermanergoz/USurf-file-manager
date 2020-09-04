package com.erman.usurf.directory.ui

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.FileModel
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel() {
    private val _path = MutableLiveData<String>().apply {
        value = ""
    }

    //TODO: It is considered a good practice to expose a LiveData instance and keep the actual
    // MutableLiveData private. Change other viewmodels accordingly!
    val path: MutableLiveData/*change to LiveData*/<String> = _path

    fun getFileList(): List<FileModel> {
        return if (!path.value.isNullOrEmpty()) {
            directoryModel.getFileModelsFromFiles(path.value!!)
        } else emptyList()
    }

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    fun onFileClick(file: FileModel) {
        if (multiSelectionMode.value!!)
            manageMultipleSelectionList(file)
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

    private val _optionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val optionMode: LiveData<Boolean> = _optionMode

    private val _multiSelectionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val multiSelectionMode: LiveData<Boolean> = _multiSelectionMode

    private val _multipleSelection = MutableLiveData<MutableList<FileModel>>().apply {
        value = mutableListOf()
    }
    val multipleSelection: LiveData<MutableList<FileModel>> = _multipleSelection

    fun onFileLongClick(file: FileModel): Boolean {
        manageMultipleSelectionList(file)
        _optionMode.value = true
        _multiSelectionMode.value = true
        return true
    }

    fun manageMultipleSelectionList(file: FileModel) {
        val selection = multipleSelection.value!!
        if (file.isSelected) {
            file.isSelected = false
            selection.remove(file)
        } else {
            file.isSelected = true
            selection.add(file)
        }
        _multipleSelection.value = selection
    }

    private val _moreOptionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val moreOptionMode: LiveData<Boolean> = _moreOptionMode

    fun onMoreClicked() {
        _moreOptionMode.value = !_moreOptionMode.value!!
    }

    fun onBackPressed(): Boolean {
        try {
            if (optionMode.value!!) {
                _optionMode.value = false
                _moreOptionMode.value = false
                _multiSelectionMode.value = false
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
}