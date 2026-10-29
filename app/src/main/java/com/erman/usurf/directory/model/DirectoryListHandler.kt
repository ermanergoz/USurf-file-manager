package com.erman.usurf.directory.model

import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val HIDDEN_FILE_PREFIX = '.'

// How many levels below a folder its size walk reads; a deeper tree is not one a person made by hand.
private const val MAX_SIZE_WALK_DEPTH = 64

// ls -a also prints the folder itself and its parent; neither is an entry to show or open.
private val DOT_ENTRIES: Set<String> = setOf(".", "..")

class DirectoryListHandler(
    private val preferencesRepository: PreferencesRepository,
    private val rootHandler: RootHandler,
    private val ioDispatcher: CoroutineDispatcher
) {
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
    fun folderSize(path: String): Long {
        // From the folder's own path with every link in it resolved, so a link inside it shows as one.
        val folder = try {
            File(path).canonicalFile
        } catch (err: IOException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            return 0
        }
        return sizeOf(folder, depth = 0)
    }

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
        size = if (file.isFile) file.length() else null,
        isDirectory = file.isDirectory,
        lastModified = file.lastModified(),
        extension = extensionOf(file),
        subFileCount = if (file.isDirectory) fileCountOf(file) else null,
        permission = getPermissions(file),
        isHidden = file.isHidden,
        isInRoot = false,
        isSelected = false
    )

    // A leading dot marks a hidden name, not an extension: ".nomedia" keeps its whole name and has no extension.
    private fun extensionOf(file: File): String = if (file.nameWithoutExtension.isEmpty()) "" else file.extension

    private fun nameWithoutExtensionOf(file: File): String = file.nameWithoutExtension.ifEmpty { file.name }

    private fun fileCountOf(folder: File): Int = folder.list()?.size ?: 0

    private fun createFileModelFromRootEntry(parentPath: String, entryName: String): FileModel {
        val isDirectory = entryName.last() == PATH_SEPARATOR_CHAR
        val cleanName = if (isDirectory) entryName.dropLast(SUFFIX_LENGTH) else entryName
        val basePath = if (parentPath == ROOT_DIRECTORY) ROOT_DIRECTORY else "$parentPath${File.separator}"
        return FileModel(
            path = "$basePath$cleanName",
            name = cleanName,
            nameWithoutExtension = cleanName,
            isDirectory = isDirectory,
            extension = "",
            permission = MountOption.OTHER,
            isHidden = entryName.first() == HIDDEN_FILE_PREFIX,
            isInRoot = true,
            isSelected = false
        )
    }

    // A folder that is gone or cannot be read holds nothing. The folders right under the file system root, as /proc
    // and /sys, are not walked: their entries are no files to count. A link is not followed, since it would count what
    // it points at a second time and one that points back up would never end, and the walk stops at its depth cap.
    private fun sizeOf(folder: File, depth: Int): Long {
        if (folder.parent == ROOT_DIRECTORY || depth > MAX_SIZE_WALK_DEPTH) return 0
        return folder.listFiles().orEmpty().filterNot(::isLink).sumOf { entry ->
            if (entry.isDirectory) sizeOf(entry, depth + 1) else entry.length()
        }
    }

    // An entry whose path, resolved, is another one is a link; one that cannot be resolved is not followed either.
    private fun isLink(entry: File): Boolean = try {
        entry.canonicalFile != entry.absoluteFile
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        true
    }
}
