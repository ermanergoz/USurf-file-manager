package com.erman.usurf.directory.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat

class DirectoryViewModel(private val directoryModel: DirectoryModel) : ViewModel() {
    private val _path = MutableLiveData<String>().apply {
        value = ""
    }
    val path: MutableLiveData<String> = _path

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

    fun onFileClick(file: File) {
        path.value = file.path
    }

    fun onBackPressed(): Boolean {
        try {
            val prevFile = File(File(path.value!!).parent!!)
            if (prevFile.canRead()) {
                path.value = prevFile.path
                return true
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }
        return false
    }
}