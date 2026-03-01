package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.R
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.EXTERNAL_SD_STORAGE_INDEX
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.erman.usurf.utils.logi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import kotlin.math.round

private const val SIMPLE_DATE_FORMAT_PATTERN = "dd MMMM | HH:mm:ss"
private const val DUMMY_FILE_NAME = "usurfIsRootCheck.txt"
private const val DUMMY_FILE_NAME_WO_EXTENSION = "usurfIsRootCheck"
private const val EXTRACTED_FOLDER_NAME_SUFFIX = "_extracted"
private const val PATH_SEPARATOR_SKIP_LENGTH = 1
private const val HIDDEN_FILE_PREFIX = '.'
private const val BUFFER_SIZE = 131072 // Ideal for most devices also considering the old devices
private const val END_OF_STREAM = -1
private const val OFFSET_START = 0
private const val TWO_DECIMAL_SCALE = 100.0
private const val TEMP_FOLDER_NAME = "usurfTemp"
private const val KILOBYTE_DIVISOR = 1024.0
private const val MEGABYTE_DIVISOR = 1024.0 * 1024.0
private const val GIGABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0
private const val TERABYTE_DIVISOR = 1024.0 * 1024.0 * 1024.0 * 1024.0
private const val INVALID_SIZE_MB: Double = -1.0

class DirectoryModel(
    private val preferencesRepository: PreferencesRepository,
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val rootHandler: RootHandler,
    private val context: Context,
    private val storagePathsProvider: StoragePathsProvider,
) {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    suspend fun getFileModelsFromDirectory(path: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val showHidden = preferencesRepository.getShowHiddenPreference()

            File(path).listFiles()?.let { fileList ->
                var filteredFileList =
                    fileList.filter { !it.isHidden || showHidden }
                        .sortedWith(compareBy({ !it.isDirectory }, { it.name })).toList()

                when (preferencesRepository.getFileSortPreference()) {
                    FileSortMethod.BY_NAME.sort ->
                        filteredFileList =
                            filteredFileList.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                                .toList()

                    FileSortMethod.BY_SIZE.sort ->
                        filteredFileList =
                            filteredFileList.sortedWith(
                                compareBy(
                                    { !it.isDirectory },
                                    { it.length() },
                                    { getFolderSize(it) },
                                ),
                            ).toList()

                    FileSortMethod.BY_LAST_MODIFIED.sort ->
                        filteredFileList =
                            filteredFileList.sortedWith(
                                compareBy(
                                    { !it.isDirectory },
                                    { it.lastModified() },
                                ),
                            ).toList()
                }

                if (preferencesRepository.getDescendingOrderPreference()) {
                    filteredFileList =
                        filteredFileList.sortedWith(compareBy { it.isDirectory }).reversed()
                }

                return@withContext filteredFileList.map { mapFileToModel(it, isInRoot = false) }
            } ?: let {
                // if it is null, it is most likely to be in a root directory
                if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                    rootHandler.getFileList(path).filter { it.first() != '.' || showHidden }.map {
                        createFileModelFromRootEntry(path, it)
                    }
                } else {
                    emptyList()
                }
            }
        }

    private fun getPermissions(file: File): MountOption {
        return when {
            file.canRead() && file.canWrite() -> MountOption.READ_WRITE
            else ->
                if (file.canRead() && !file.canWrite()) {
                    MountOption.READ
                } else {
                    MountOption.OTHER
                }
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

    private fun Double.round(): Double =
        try {
            round(this * TWO_DECIMAL_SCALE) / TWO_DECIMAL_SCALE
        } catch (err: NumberFormatException) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
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
            terabyte > 1 -> "${terabyte.round()} ${context.getString(R.string.tb)}"
            gigabyte > 1 -> "${gigabyte.round()} ${context.getString(R.string.gb)}"
            megabyte > 1 -> "${megabyte.round()} ${context.getString(R.string.mb)}"
            kilobyte > 1 -> "${kilobyte.round()} ${context.getString(R.string.kb)}"
            else -> "$size ${context.getString(R.string.bytes)}"
        }
    }

    private fun getFolderSize(file: File): Double {
        if (file.exists() && file.parent != ROOT_DIRECTORY) {
            file.listFiles()?.let {
                var folderSize = Double.NaN

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

    fun manageMultipleSelectionList(
        file: FileModel,
        multipleSelection: List<FileModel>,
    ): List<FileModel> =
        if (file.isSelected) {
            multipleSelection.filter { it.path != file.path }
        } else {
            multipleSelection + file.copy(isSelected = true)
        }

    @Suppress("NestedBlockDepth", "ReturnCount")
    private fun getDocumentFile(
        file: File,
        isDirectory: Boolean,
    ): DocumentFile? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)
        val getExtSdCardBaseFolder: String
        try {
            getExtSdCardBaseFolder =
                storagePathsProvider.getStorageDirectories().elementAt(EXTERNAL_SD_STORAGE_INDEX)
        } catch (err: IndexOutOfBoundsException) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            return null
        }
        var originalDirectory = false
        var relativePathOfFile: String? = null
        try {
            val fullPath = file.canonicalPath
            if (getExtSdCardBaseFolder != fullPath) {
                relativePathOfFile =
                    fullPath.substring(getExtSdCardBaseFolder.length + PATH_SEPARATOR_SKIP_LENGTH)
            } else {
                originalDirectory = true
            }
        } catch (err: IOException) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            return null
        } catch (err: Exception) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            originalDirectory = true
        }

        val extSdCardChosenUri = storageDirectoryRepository.getChosenUri()
        var treeUri: Uri? = null
        if (extSdCardChosenUri != null) treeUri = extSdCardChosenUri.toUri()
        if (treeUri == null) {
            return null
        }
        // start with root of SD card and then parse through document tree.
        var document: DocumentFile?
        try {
            document = DocumentFile.fromTreeUri(context, treeUri)
        } catch (err: Exception) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            return null
        }
        if (originalDirectory) return document
        relativePathOfFile?.split(File.separator.toRegex())?.dropLastWhile { it.isEmpty() }
            ?.toTypedArray()?.let { parts ->
                for (i in parts.indices) {
                    document?.let {
                        var nextDocument = it.findFile(parts[i])
                        if (nextDocument == null) {
                            nextDocument =
                                if (i < parts.size - 1 || isDirectory) {
                                    it.createDirectory(parts[i])
                                } else {
                                    it.createFile(MIME_TYPE_ALL, parts[i])
                                }
                        }
                        document = nextDocument
                    }
                }
            }
        return document
    }

    private suspend fun isRootDirectory(path: String): Boolean =
        withContext(Dispatchers.IO) {
            // check if the directory is in root
            val pathFile = File(path)

            val dummyFilePath =
                if (pathFile.isDirectory) {
                    pathFile.path
                } else {
                    "" + pathFile.parent
                }

            try {
                withContext(Dispatchers.IO) { File(dummyFilePath + File.separator + DUMMY_FILE_NAME).createNewFile() }
            } catch (err: Exception) {
                err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                getDocumentFile(File(dummyFilePath), File(dummyFilePath).isDirectory)?.createFile(
                    MIME_TYPE_ALL,
                    DUMMY_FILE_NAME,
                )
            }
            val isRoot: Boolean = !File(dummyFilePath + File.separator + DUMMY_FILE_NAME).exists()

            if (!isRoot && !File(dummyFilePath + File.separator + DUMMY_FILE_NAME).delete()) {
                val documentFileToDelete =
                    getDocumentFile(
                        File(dummyFilePath + File.separator + DUMMY_FILE_NAME),
                        File(dummyFilePath + File.separator + DUMMY_FILE_NAME).isDirectory,
                    )
                documentFileToDelete?.let {
                    deleteFolderRecursively(it)
                }
            } else if (isRoot) {
                val toDelete = getSearchedDeviceFiles(DUMMY_FILE_NAME_WO_EXTENSION)
                if (toDelete.isNotEmpty()) delete(toDelete)
            }
            return@withContext isRoot
        }

    suspend fun rename(
        selectedDirectory: FileModel,
        newFileName: String,
    ) = withContext(Dispatchers.IO) {
        val dirName = File(selectedDirectory.path).parent
        if (selectedDirectory.name == newFileName) cancel()

        if (isRootDirectory(selectedDirectory.path)) {
            // do it with root permissions
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.renameFile(selectedDirectory, newFileName)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            }
        } else {
            // normal way
            val prev = File(dirName, selectedDirectory.name)
            val new = File(dirName, newFileName)
            if (!prev.renameTo(new)) {
                val documentFile =
                    getDocumentFile(File(selectedDirectory.path), selectedDirectory.isDirectory)
                documentFile?.renameTo(newFileName)
            }
            // if the normal way doesn't work, try with SAF
            if (!File("$dirName${File.separator}$newFileName").exists()) {
                cancel()
            }
        }
    }

    suspend fun delete(selectedDirectories: List<FileModel>) =
        withContext(Dispatchers.IO) {
            if (selectedDirectories.isEmpty()) return@withContext
            if (isRootDirectory(selectedDirectories.first().path)) {
                // do it with root permissions
                if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                    rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                    val isSuccess = rootHandler.delete(selectedDirectories)
                    rootHandler.remountRootDirAs(MountOption.READ.option)
                    if (!isSuccess) cancel()
                } else {
                    cancel()
                }
            } else {
                for (i in selectedDirectories.indices) {
                    // normal way
                    val isSuccess =
                        if (selectedDirectories[i].isDirectory) {
                            File(selectedDirectories[i].path).deleteRecursively()
                        } else {
                            File(selectedDirectories[i].path).delete()
                        }
                    if (!isSuccess) {
                        val documentFileToDelete =
                            getDocumentFile(
                                File(selectedDirectories[i].path),
                                selectedDirectories[i].isDirectory,
                            )
                        // if the normal way doesn't work, try with SAF
                        if (documentFileToDelete != null &&
                            !deleteFolderRecursively(
                                documentFileToDelete,
                            )
                        ) {
                            cancel()
                        }
                    }
                }
            }
        }

    private fun deleteFolderRecursively(documentFile: DocumentFile): Boolean {
        if (documentFile.listFiles().isNotEmpty()) {
            for (i in documentFile.listFiles().size - 1 downTo OFFSET_START) {
                deleteFolderRecursively(documentFile.listFiles()[i])
            }
        }
        documentFile.delete()
        return !documentFile.exists()
    }

    suspend fun createFolder(
        path: String,
        folderName: String,
    ) = withContext(Dispatchers.IO) {
        if (isRootDirectory(path)) {
            // do it with root permissions
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.createFolder(path, folderName)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            } else {
                cancel()
            }
        } else {
            if (!File("$path${File.separator}$folderName").exists()) {
                // normal way
                // try catch wont work here. Doesn't throw IOException
                if (!File("$path${File.separator}$folderName").mkdir()) {
                    // if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createDirectory(folderName)
                    if (!File("$path${File.separator}$folderName").exists()) {
                        cancel()
                    } else {
                        Unit
                    }
                }
            } else {
                cancel()
            }
        }
    }

    suspend fun createFile(path: String, fileName: String, ) = withContext(Dispatchers.IO) {
        if (isRootDirectory(path)) {
            // do it with root permissions
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.createFile(path, fileName)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) {
                    cancel()
                } else {
                    Unit
                }
            } else {
                cancel()
            }
        } else {
            if (!File("$path${File.separator}$fileName").exists()) {
                try {
                    // if wont work here. Throws IOException
                    // normal way
                    File("$path${File.separator}$fileName").createNewFile()
                } catch (err: Exception) {
                    err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                    // if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createFile(
                        MIME_TYPE_ALL,
                        fileName,
                    )
                    if (!File("$path${File.separator}$fileName").exists()) {
                        cancel()
                    } else {
                        Unit
                    }
                }
            } else {
                cancel()
            }
        }
    }

    private fun doesFileExist(
        fileModel: FileModel,
        copyOrMoveDestination: String,
    ): Boolean {
        val files = File(copyOrMoveDestination).listFiles()

        if (files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.name == fileModel.name) return true
            }
        }
        return false
    }

    suspend fun copyFile(
        copyOrMoveSources: List<FileModel>,
        copyOrMoveDestination: String,
    ) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (isRootDirectory(copyOrMoveDestination) || isSourceInRoot) {
            // do it with root permissions
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.copyFile(copyOrMoveSources, copyOrMoveDestination)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            } else {
                cancel()
            }
        } else {
            for (i in copyOrMoveSources.indices) {
                logi("Attempt to copy: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
                if (!doesFileExist(copyOrMoveSources[i], copyOrMoveDestination)) {
                    // normal way
                    if (copyOrMoveSources[i].isDirectory) {
                        try {
                            File(copyOrMoveSources[i].path).copyRecursively(
                                File(
                                    copyOrMoveDestination + File.separator + copyOrMoveSources[i].name,
                                ),
                            )
                        } catch (err: Exception) {
                            if (!copyToExtCard(
                                    File(copyOrMoveSources[i].path),
                                    copyOrMoveDestination,
                                )
                            ) {
                                cancel()
                            }
                        }
                    } else {
                        try {
                            File(
                                copyOrMoveSources[i].path,
                            ).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                        } catch (err: IOException) {
                            // with SAF
                            if (!copyToExtCard(
                                    File(copyOrMoveSources[i].path),
                                    copyOrMoveDestination,
                                )
                            ) {
                                cancel()
                            }
                        }
                    }
                } else {
                    cancel()
                }
            }
        }
    }

    private fun copyToExtCard(
        sourceFile: File,
        copyOrMoveDestination: String,
    ): Boolean {
        var documentFileDestination: DocumentFile? =
            getDocumentFile(File(copyOrMoveDestination), File(copyOrMoveDestination).isDirectory)
        var fileInputStream: FileInputStream? = null
        var outputStream: OutputStream? = null
        var isSuccess = true

        if (sourceFile.isDirectory) {
            documentFileDestination?.createDirectory(sourceFile.name)
            sourceFile.listFiles()?.let { sourceFiles ->
                for (i in sourceFiles.indices) {
                    isSuccess =
                        copyToExtCard(
                            sourceFiles[i],
                            copyOrMoveDestination + File.separator + sourceFile.name,
                        )
                }
            }
            return isSuccess
        } else {
            documentFileDestination?.createFile(sourceFile.extension, sourceFile.name)?.let {
                documentFileDestination = it
            }
            try {
                fileInputStream = FileInputStream(sourceFile)
                outputStream =
                    documentFileDestination?.uri?.let {
                        context.contentResolver.openOutputStream(it)
                    }
                val byteArray = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                try {
                    while (fileInputStream.read(byteArray)
                            .also { bytesRead = it } != END_OF_STREAM
                    ) {
                        outputStream?.write(byteArray, OFFSET_START, bytesRead)
                    }
                } catch (err: Exception) {
                    err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                    isSuccess = false
                } finally {
                    try {
                        fileInputStream.close()
                        outputStream?.close()
                    } catch (err: Exception) {
                        err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                        isSuccess = false
                    }
                }
            } catch (err: Exception) {
                err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                isSuccess = false
            } finally {
                try {
                    fileInputStream?.close()
                    outputStream?.close()
                } catch (err: Exception) {
                    err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
                    isSuccess = false
                }
            }
            return isSuccess
        }
    }

    suspend fun moveFile(
        copyOrMoveSources: List<FileModel>,
        copyOrMoveDestination: String,
    ) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (isRootDirectory(copyOrMoveDestination) || isSourceInRoot) {
            // do it with root permissions
            if (preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.moveFile(copyOrMoveSources, copyOrMoveDestination)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            } else {
                cancel()
            }
        } else {
            copyFile(copyOrMoveSources, copyOrMoveDestination)
            delete(copyOrMoveSources)
        }
    }

    @SuppressLint("DefaultLocale")
    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val fileList = mutableListOf<File>()
            try {
                val storagePaths = storagePathsProvider.getStorageDirectories()
                for (storagePath in storagePaths) {
                    if (storagePath != File.separator) {
                        fileList.addAll(
                            searchRecursively(
                                File(storagePath),
                                searchQuery,
                            ),
                        )
                    }
                }
                return@withContext fileList.filter { file ->
                    searchQuery.lowercase().toRegex()
                        .containsMatchIn(file.nameWithoutExtension.lowercase())
                }.map { mapFileToModel(it) }
            } catch (err: java.lang.Exception) {
                err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            }
            return@withContext emptyList<FileModel>()
        }

    private fun searchRecursively(
        directory: File,
        searchQuery: String,
        res: MutableSet<File> = mutableSetOf(),
    ): Set<File> {
        // Depth first search algorithm
        directory.listFiles()?.let { fileList ->
            for (file in fileList.toSet()) {
                res.add(file)
                if (file.isDirectory) {
                    searchRecursively(file, searchQuery, res)
                }
            }
        }
        return res
    }

    suspend fun compressFiles(
        multipleSelection: List<FileModel>,
        compressedFileNameWithExtension: String,
    ) = withContext(Dispatchers.IO) {
        if (multipleSelection.isEmpty()) return@withContext
        val parentPath: String = File(multipleSelection.first().path).parent ?: ""
        val archiveType: String =
            compressedFileNameWithExtension.substring(
                compressedFileNameWithExtension.lastIndexOf(FILE_EXTENSION_SEPARATOR),
            ).drop(SUFFIX_LENGTH)

        if (!FileCompressionHandler().compress(
                "$parentPath${File.separator}$compressedFileNameWithExtension",
                multipleSelection,
                archiveType,
            )
        ) {
            createFolder(context.cacheDir.absolutePath, TEMP_FOLDER_NAME)
            val cachedFolderDirectory =
                context.cacheDir.absolutePath + File.separator + TEMP_FOLDER_NAME
            val compressedFileDirectory =
                "$cachedFolderDirectory${File.separator}$compressedFileNameWithExtension"
            copyFile(multipleSelection, cachedFolderDirectory)

            if (FileCompressionHandler().compress(
                    compressedFileDirectory,
                    getFileModelsFromDirectory(cachedFolderDirectory).toMutableList(),
                    archiveType,
                )
            ) {
                moveFile(
                    listOf(
                        FileModel(
                            path = compressedFileDirectory,
                            name = compressedFileNameWithExtension,
                            isDirectory = false,
                        ),
                    ),
                    parentPath,
                )
            } else {
                cancel()
            }
            delete(
                listOf(
                    FileModel(
                        path = cachedFolderDirectory,
                        name = TEMP_FOLDER_NAME,
                        isDirectory = true,
                    ),
                ),
            )
        }
    }

    suspend fun extractFiles(selectedDirectory: FileModel) =
        withContext(Dispatchers.IO) {
            val parentPath: String = File(selectedDirectory.path).parent ?: ""
            val extractedFolderName =
                selectedDirectory.nameWithoutExtension + EXTRACTED_FOLDER_NAME_SUFFIX
            createFolder(parentPath, extractedFolderName)
            if (!FileCompressionHandler().extract(
                    selectedDirectory.path,
                    "$parentPath${File.separator}$extractedFolderName",
                )
            ) {
                createFolder(context.cacheDir.absolutePath, TEMP_FOLDER_NAME)
                val cachedFolderDirectory =
                    context.cacheDir.absolutePath + File.separator + TEMP_FOLDER_NAME
                val cachedCompressedFileDirectory =
                    cachedFolderDirectory + File.separator + selectedDirectory.name
                copyFile(listOf(selectedDirectory), cachedFolderDirectory)
                createFolder(cachedFolderDirectory, extractedFolderName)
                val cachedExtractedFolderDirectory =
                    cachedFolderDirectory + File.separator + extractedFolderName
                if (FileCompressionHandler().extract(
                        cachedCompressedFileDirectory,
                        cachedExtractedFolderDirectory,
                    )
                ) {
                    moveFile(
                        getFileModelsFromDirectory(cachedExtractedFolderDirectory).toMutableList(),
                        "$parentPath${File.separator}$extractedFolderName",
                    )
                } else {
                    cancel()
                }
                delete(
                    listOf(
                        FileModel(
                            path = cachedFolderDirectory,
                            name = TEMP_FOLDER_NAME,
                            isDirectory = true,
                        ),
                    ),
                )
            }
        }
}
