package com.erman.usurf.directory.ui

import android.content.ActivityNotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.utils.Event
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

    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)
    fun getLastModified(file: File): String {
        return dateFormat.format(file.lastModified())
    }

    fun getFileExtension(file: File): String {
        return if (file.isFile) file.extension
        else ""
    }

    fun getFileSize(file: File): String {
        return directoryModel.getConvertedFileSize(file.length())
    }

    fun getSubFileCount(file: File): String {
        return try {
            val fileCount = File(file.path).listFiles()!!.size
            if (fileCount == 0) "Empty Folder"
            else "$fileCount files"
        } catch (err: Exception) {
            err.printStackTrace()
            ""
        }
    }

    fun getFileList(): List<File> {
        return if (!path.value.isNullOrEmpty()) {
            directoryModel.getFiles(path.value!!)
        } else emptyList()
    }

    private val _toastMessage = MutableLiveData<Event<Int>>()
    val toastMessage: LiveData<Event<Int>> = _toastMessage

    fun onFileClick(file: File) {
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

    private val _optionMode = MutableLiveData<Boolean>().apply {
        value = false
    }
    val optionMode: LiveData<Boolean> = _optionMode

    fun onFileLongClick(file: File): Boolean {
        _optionMode.value = true
        return true
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