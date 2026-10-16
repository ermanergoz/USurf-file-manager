package com.erman.usurf.directory.model

import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge

class FileCompressionHandler(
    private val executor: SevenZipExecutor = SevenZipExecutor { arguments -> P7ZipApi.executeCommand(arguments) }
) {
    fun compress(compressedFileDirectory: String, multipleSelection: List<FileModel>, archiveType: String): Boolean {
        val arguments = SevenZipCommand.compress(compressedFileDirectory, multipleSelection.map { it.path }, archiveType)
        when (executor.execute(arguments)) {
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
        when (executor.execute(SevenZipCommand.extract(compressedFileDirectory, outputDirectory))) {
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
