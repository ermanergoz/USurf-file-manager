package com.erman.usurf.directory.model

import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import com.hzy.libp7zip.P7ZipApi

class FileCompressionHandler {
    fun compress(compressedFileDirectory: String, multipleSelection: MutableList<FileModel>, archiveType: String): Boolean {
        var filesToBeCompressed = ""
        for (file in multipleSelection) {
            filesToBeCompressed = filesToBeCompressed + " '" + file.path + "'"
        }

        when (P7ZipApi.executeCommand("7z a -t$archiveType -mx=9 '$compressedFileDirectory' $filesToBeCompressed")) {
            0 -> {
                logd("No errors or warnings detected")
                return true
            }
            1 -> {
                logd("Warning (Non fatal error(s))")
                return true
            }
            2 -> loge("Fatal error")
            7 -> loge("Bad command line parameters")
            8 -> loge("Not enough memory for operation")
            255 -> loge("The process has been stopped")
        }

        return false
    }

    fun extract(compressedFileDirectory: String, outputDirectory: String): Boolean {
        when (P7ZipApi.executeCommand("7z x '$compressedFileDirectory' '-o$outputDirectory'")) {
            0 -> {
                logd("No errors or warnings detected")
                return true
            }
            1 -> {
                logd("Warning (Non fatal error(s))")
                return true
            }
            2 -> loge("Fatal error")
            7 -> loge("Bad command line parameters")
            8 -> loge("Not enough memory for operation")
            255 -> loge("The process has been stopped")
        }

        return false
    }
}