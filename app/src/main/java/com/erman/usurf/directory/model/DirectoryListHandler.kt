package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.content.Context
import com.erman.usurf.R
import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round

private const val SIMPLE_DATE_FORMAT_PATTERN = "dd MMMM | HH:mm:ss"
private const val HIDDEN_FILE_PREFIX = '.'
private const val TWO_DECIMAL_SCALE = 100.0
private const val KILOBYTE_DIVISOR = 1024.0
private const val MEGABYTE_DIVISOR = 1024.0 * 1024.0
private const val GIGABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0
private const val TERABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0 * 1024.0
private const val INVALID_SIZE_MB: Double = -1.0

class DirectoryListHandler(
    private val preferencesRepository: PreferencesRepository,
    private val rootHandler: RootHandler,
    private val context: Context,
) {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    suspend fun getFileModelsFromDirectory(path: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val fileList = File(path).listFiles()
            if (fileList != null) {
                getFileModelsFromNormalDirectory(fileList)
            } else {
                getFileModelsFromRootDirectory(path)
            }
        }

    fun mapFilesToFileModels(files: List<File>): List<FileModel> = files.map { mapFileToModel(it, isInRoot = false) }

    private suspend fun getFileModelsFromNormalDirectory(fileList: Array<File>): List<FileModel> =
        withContext(Dispatchers.IO) {
            val showHidden = preferencesRepository.getShowHiddenPreference()
            val filteredFiles = filterHiddenFiles(fileList, showHidden)
            val sortedFiles = applyFileSorting(filteredFiles)
            return@withContext sortedFiles.map { mapFileToModel(it, isInRoot = false) }
        }

    private suspend fun getFileModelsFromRootDirectory(path: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val showHidden = preferencesRepository.getShowHiddenPreference()
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.getFileList(path)
                    .filter { it.first() != HIDDEN_FILE_PREFIX || showHidden }
                    .map { createFileModelFromRootEntry(path, it) }
            } else {
                throw CancellationException()
            }
        }

    private fun filterHiddenFiles(
        fileList: Array<File>,
        showHidden: Boolean,
    ): List<File> = fileList.filter { !it.isHidden || showHidden }

    private fun applyFileSorting(fileList: List<File>): List<File> {
        val sortedByMethod = sortFilesBy(fileList)
        return if (preferencesRepository.getDescendingOrderPreference()) {
            sortedByMethod.sortedWith(compareBy { it.isDirectory }).reversed()
        } else {
            sortedByMethod
        }
    }

    private fun sortFilesBy(fileList: List<File>): List<File> {
        return when (preferencesRepository.getFileSortPreference()) {
            FileSortMethod.BY_NAME.sort ->
                fileList.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

            FileSortMethod.BY_SIZE.sort ->
                fileList.sortedWith(
                    compareBy(
                        { !it.isDirectory },
                        { it.length() },
                        { getFolderSize(it) },
                    ),
                )

            FileSortMethod.BY_LAST_MODIFIED.sort ->
                fileList.sortedWith(
                    compareBy(
                        { !it.isDirectory },
                        { it.lastModified() },
                    ),
                )

            else -> fileList.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        }
    }

    private fun getPermissions(file: File): MountOption {
        return when {
            file.canRead() && file.canWrite() -> MountOption.READ_WRITE
            file.canRead() && !file.canWrite() -> MountOption.READ
            else -> MountOption.OTHER
        }
    }

    private fun mapFileToModel(
        file: File,
        isInRoot: Boolean = false,
    ): FileModel =
        FileModel(
            path = file.path,
            name = file.name,
            nameWithoutExtension = file.nameWithoutExtension,
            size = getConvertedFileSize(file),
            isDirectory = file.isDirectory,
            lastModified = dateFormat.format(file.lastModified()),
            extension = file.extension,
            subFileCount = "${file.listFiles()?.size ?: 0} ${context.getString(R.string.file_count)}",
            permission = getPermissions(file),
            isHidden = file.isHidden,
            isInRoot = isInRoot,
            isSelected = false,
        )

    private fun createFileModelFromRootEntry(
        parentPath: String,
        entryName: String,
    ): FileModel {
        val isDirectory = entryName.last() == PATH_SEPARATOR_CHAR
        val cleanName = if (isDirectory) entryName.dropLast(SUFFIX_LENGTH) else entryName
        val basePath = if (parentPath == ROOT_DIRECTORY) ROOT_DIRECTORY else "$parentPath${File.separator}"
        return FileModel(
            path = "$basePath$cleanName",
            name = cleanName,
            nameWithoutExtension = cleanName,
            size = "",
            isDirectory = isDirectory,
            lastModified = "",
            extension = "",
            subFileCount = "",
            permission = MountOption.OTHER,
            isHidden = entryName.first() == HIDDEN_FILE_PREFIX,
            isInRoot = true,
            isSelected = false,
        )
    }

    private fun Double.roundSize(): Double =
        try {
            round(this * TWO_DECIMAL_SCALE) / TWO_DECIMAL_SCALE
        } catch (err: NumberFormatException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            INVALID_SIZE_MB
        }

    private fun getConvertedFileSize(file: File): String {
        val size: Long =
            if (file.isFile) {
                file.length()
            } else {
                getFolderSize(file).toLong()
            }
        val kilobyte = size / KILOBYTE_DIVISOR
        val megabyte = size / MEGABYTE_DIVISOR
        val gigabyte = size / GIGABYTE_DIVISOR
        val terabyte = size / TERABYTE_DIVISOR
        return when {
            terabyte > 1 -> "${terabyte.roundSize()} ${context.getString(R.string.tb)}"
            gigabyte > 1 -> "${gigabyte.roundSize()} ${context.getString(R.string.gb)}"
            megabyte > 1 -> "${megabyte.roundSize()} ${context.getString(R.string.mb)}"
            kilobyte > 1 -> "${kilobyte.roundSize()} ${context.getString(R.string.kb)}"
            else -> "$size ${context.getString(R.string.bytes)}"
        }
    }

    private fun getFolderSize(file: File): Double {
        if (file.exists() && file.parent != ROOT_DIRECTORY) {
            file.listFiles()?.let {
                var folderSize = 0.0
                it.forEach { subFile ->
                    if (subFile.isDirectory) {
                        folderSize += getFolderSize(subFile)
                    } else {
                        folderSize += subFile.length()
                    }
                }
                return folderSize
            }
        }
        return INVALID_SIZE_MB
    }
}
