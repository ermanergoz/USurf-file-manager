package com.erman.usurf.directory.model

import android.content.Intent
import androidx.core.content.FileProvider
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import java.io.File
import java.text.SimpleDateFormat

class DirectoryModel {
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    fun getFileModelsFromFiles(path: String): List<FileModel> {
        val files = File(path).listFiles().toList()
        return files.map {
            FileModel(it.path, it.name, getConvertedFileSize(it.length()), it.isDirectory,
                dateFormat.format(it.lastModified()), it.extension,
                (it.listFiles()?.size.toString() + " files"), false)
        }
    }

    fun getFolderSize(path: String): Double {
        if (File(path).exists()) {
            var size = 0.0
            var fileList = File(path).listFiles().toList()

            for (i in fileList.indices) {
                if (fileList[i].isDirectory)
                    size += getFolderSize(fileList[i].path)
                else size += fileList[i].length()
            }
            return size
        }
        return 0.0
    }

    private fun getConvertedFileSize(size: Long): String {
        var sizeStr = ""

        val kilobyte = size / 1024.0
        val megabyte = size / (1024.0 * 1024.0)
        val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
        val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

        sizeStr = when {
            terabyte > 1 -> "%.2f".format(terabyte) + " TB"
            gigabyte > 1 -> "%.2f".format(gigabyte) + " GB"
            megabyte > 1 -> "%.2f".format(megabyte) + " MB"
            kilobyte > 1 -> "%.2f".format(kilobyte) + " KB"
            else -> size.toDouble().toString() + " Bytes"
        }
        return sizeStr
    }

    fun getConvertedFileSize(size: Double): String {
        var sizeStr = ""

        val kilobyte = size / 1024.0
        val megabyte = size / (1024.0 * 1024.0)
        val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
        val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

        sizeStr = when {
            terabyte > 1 -> "%.2f".format(terabyte) + " TB"
            gigabyte > 1 -> "%.2f".format(gigabyte) + " GB"
            megabyte > 1 -> "%.2f".format(megabyte) + " MB"
            kilobyte > 1 -> "%.2f".format(kilobyte) + " KB"
            else -> size.toDouble().toString() + " Bytes"
        }
        return sizeStr
    }

    fun openFile(directory: FileModel) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = FileProvider.getUriForFile(appContext, appContext.packageName, File(directory.path))
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun manageMultipleSelectionList(file: FileModel, multipleSelection: MutableList<FileModel>): MutableList<FileModel> {
        if (file.isSelected) {
            file.isSelected = false
            multipleSelection.remove(file)
        } else {
            file.isSelected = true
            multipleSelection.add(file)
        }
        return multipleSelection
    }

    fun clearMultipleSelection(multipleSelection: MutableList<FileModel>): MutableList<FileModel> {
        for (i in multipleSelection.indices) {
            multipleSelection[i].isSelected = false
        }
        multipleSelection.clear()
        return multipleSelection
    }
}