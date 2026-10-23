package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.content.Context
import com.erman.usurf.R
import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.utils.ROOT_DIRECTORY
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.round
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val SIMPLE_DATE_FORMAT_PATTERN = "dd MMMM | HH:mm:ss"
private const val HIDDEN_FILE_PREFIX = '.'
private const val TWO_DECIMAL_SCALE = 100.0
private const val KILOBYTE_DIVISOR = 1024.0
private const val MEGABYTE_DIVISOR = 1024.0 * 1024.0
private const val GIGABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0
private const val TERABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0 * 1024.0

// ls -a also prints the folder itself and its parent; neither is an entry to show or open.
private val DOT_ENTRIES: Set<String> = setOf(".", "..")

class DirectoryListHandler(
    private val preferencesRepository: PreferencesRepository,
    private val rootHandler: RootHandler,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    suspend fun getFileModelsFromDirectory(path: String): List<FileModel> = withContext(ioDispatcher) {
        val fileList = File(path).listFiles()
        if (fileList != null) {
            getFileModelsFromNormalDirectory(fileList)
        } else {
            getFileModelsFromRootDirectory(path)
        }
    }

    fun mapFilesToFileModels(files: List<File>): List<FileModel> = files.map { mapFileToModel(it) }

    /** The size of the folder at [path]: the bytes of every file under it, read only when asked for. */
    fun folderSizeText(path: String): String = sizeText(sizeOf(File(path)))

    private fun getFileModelsFromNormalDirectory(fileList: Array<File>): List<FileModel> {
        val showHidden = preferencesRepository.getShowHiddenPreference()
        val filteredFiles = filterHiddenFiles(fileList, showHidden)
        val sortedFiles = applyFileSorting(filteredFiles)
        return sortedFiles.map { mapFileToModel(it) }
    }

    private fun getFileModelsFromRootDirectory(path: String): List<FileModel> {
        val showHidden = preferencesRepository.getShowHiddenPreference()
        return if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
            rootHandler.getFileList(path)
                .filter { it.removeSuffix(PATH_SEPARATOR_CHAR.toString()) !in DOT_ENTRIES }
                .filter { it.first() != HIDDEN_FILE_PREFIX || showHidden }
                .map { createFileModelFromRootEntry(path, it) }
        } else if (File(path).exists()) {
            throw FileOperationException("Root access is required to list this directory", OperationError.RootAccessRequired)
        } else {
            throw FileOperationException("The directory does not exist")
        }
    }

    private fun filterHiddenFiles(fileList: Array<File>, showHidden: Boolean): List<File> = fileList.filter { !it.isHidden || showHidden }

    private fun applyFileSorting(fileList: List<File>): List<File> {
        val sortedByMethod = sortFilesBy(fileList)
        return if (preferencesRepository.getDescendingOrderPreference()) {
            sortedByMethod.sortedWith(compareBy { it.isDirectory }).reversed()
        } else {
            sortedByMethod
        }
    }

    private fun sortFilesBy(fileList: List<File>): List<File> = when (preferencesRepository.getFileSortPreference()) {
        FileSortMethod.BY_NAME.sort ->
            fileList.sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        // A folder's size would take a walk of everything in it, so folders keep their names' order.
        FileSortMethod.BY_SIZE.sort ->
            fileList.sortedWith(
                compareBy(
                    { !it.isDirectory },
                    { if (it.isDirectory) 0L else it.length() },
                    { it.name }
                )
            )

        FileSortMethod.BY_LAST_MODIFIED.sort ->
            fileList.sortedWith(
                compareBy(
                    { !it.isDirectory },
                    { it.lastModified() }
                )
            )

        else -> fileList.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    private fun getPermissions(file: File): MountOption = when {
        file.canRead() && file.canWrite() -> MountOption.READ_WRITE
        file.canRead() && !file.canWrite() -> MountOption.READ
        else -> MountOption.OTHER
    }

    // A row reads each entry once: a folder shows how many entries it holds, from their names alone, and its size is
    // read only when its Information asks for it.
    private fun mapFileToModel(file: File): FileModel = FileModel(
        path = file.path,
        name = file.name,
        nameWithoutExtension = nameWithoutExtensionOf(file),
        size = if (file.isFile) sizeText(file.length()) else "",
        isDirectory = file.isDirectory,
        lastModified = dateFormat.format(file.lastModified()),
        extension = extensionOf(file),
        subFileCount = fileCountOf(file),
        permission = getPermissions(file),
        isHidden = file.isHidden,
        isInRoot = false,
        isSelected = false
    )

    // A leading dot marks a hidden name, not an extension: ".nomedia" keeps its whole name and has no extension.
    private fun extensionOf(file: File): String = if (file.nameWithoutExtension.isEmpty()) "" else file.extension

    private fun nameWithoutExtensionOf(file: File): String = file.nameWithoutExtension.ifEmpty { file.name }

    private fun fileCountOf(folder: File): String {
        val count = folder.list()?.size ?: 0
        return context.resources.getQuantityString(R.plurals.file_count, count, count)
    }

    private fun createFileModelFromRootEntry(parentPath: String, entryName: String): FileModel {
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
            isSelected = false
        )
    }

    private fun Double.roundSize(): Double = round(this * TWO_DECIMAL_SCALE) / TWO_DECIMAL_SCALE

    private fun sizeText(size: Long): String {
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

    // A folder that is gone or cannot be read holds nothing. The folders right under the file system root, as /proc
    // and /sys, are not walked: their entries are no files to count.
    private fun sizeOf(folder: File): Long {
        if (folder.parent == ROOT_DIRECTORY) return 0
        return folder.listFiles().orEmpty().sumOf { entry -> if (entry.isDirectory) sizeOf(entry) else entry.length() }
    }
}
