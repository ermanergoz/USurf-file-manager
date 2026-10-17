package com.erman.usurf.directory.model

import com.erman.usurf.R
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FileCompressionHandler(private val executor: SevenZipExecutor) {
    // 7-Zip keeps its console state in globals, so two runs at once would share it. One handler per process (a Koin
    // single) runs one command at a time; a second caller waits here without holding a thread.
    private val sevenZipLock = Mutex()

    /**
     * Returns false when 7-Zip could not write the archive, so the caller may retry through the cache folder.
     * Throws when 7-Zip skipped some inputs: retrying cannot add them.
     */
    suspend fun compress(compressedFileDirectory: String, multipleSelection: List<FileModel>, archiveType: String): Boolean =
        when (run(SevenZipCommand.compress(compressedFileDirectory, multipleSelection.map { it.path }, archiveType))) {
            SevenZipExitCode.SUCCESS -> isWritten(File(compressedFileDirectory))
            SevenZipExitCode.WARNING ->
                throw FileOperationException("7-Zip skipped some of the files", messageResId = R.string.some_files_could_not_be_added)
            else -> false
        }

    /** Returns false when 7-Zip failed; throws when it reported a warning, as part of the files may be written. */
    suspend fun extract(compressedFileDirectory: String, outputDirectory: String): Boolean =
        when (run(SevenZipCommand.extract(compressedFileDirectory, outputDirectory))) {
            SevenZipExitCode.SUCCESS -> true
            SevenZipExitCode.WARNING -> throw FileOperationException("7-Zip reported a warning while extracting")
            else -> false
        }

    // Exit code 0 means every input was added, so a written archive is never an empty one; a missing or zero-byte
    // file means nothing reached the disk.
    private fun isWritten(archive: File): Boolean {
        if (archive.length() > 0) return true
        loge("7-Zip reported success but wrote no archive")
        archive.delete()
        return false
    }

    private suspend fun run(arguments: Array<String>): SevenZipExitCode? {
        val code = sevenZipLock.withLock { executor.execute(arguments) }
        return SevenZipExitCode.fromCode(code).also { exitCode ->
            if (exitCode == SevenZipExitCode.SUCCESS) logd("7-Zip finished without errors") else loge("7-Zip exit code $code: $exitCode")
        }
    }
}
