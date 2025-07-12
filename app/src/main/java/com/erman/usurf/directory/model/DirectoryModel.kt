package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.R
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.directory.utils.BUFFER_SIZE
import com.erman.usurf.directory.utils.DUMMY_FILE_NAME
import com.erman.usurf.directory.utils.DUMMY_FILE_NAME_WO_EXTENSION
import com.erman.usurf.directory.utils.EXTRACTED_FOLDER_NAME_SUFFIX
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.FOLDER_SUFFIX
import com.erman.usurf.directory.utils.GIGABYTE_DIVISOR
import com.erman.usurf.directory.utils.HIDDEN_FILE_PREFIX
import com.erman.usurf.directory.utils.KILOBYTE_DIVISOR
import com.erman.usurf.directory.utils.MEGABYTE_DIVISOR
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.directory.utils.TERABYTE_DIVISOR
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import com.erman.usurf.utils.logi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat

class DirectoryModel(
    private val preferenceProvider: PreferenceProvider,
    private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
    private val rootHandler: RootHandler,
    private val context: Context
) {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    suspend fun getFileModelsFromFiles(path: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val showHidden = preferenceProvider.getShowHiddenPreference()

            File(path).listFiles()?.let { fileList ->
                var filteredFileList =
                    fileList.filter { !it.isHidden || showHidden }
                        .sortedWith(compareBy({ !it.isDirectory }, { it.name })).toList()

                when (preferenceProvider.getFileSortPreference()) {
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

                if (preferenceProvider.getDescendingOrderPreference()) {
                    filteredFileList =
                        filteredFileList.sortedWith(compareBy { it.isDirectory }).reversed()
                }

                return@withContext filteredFileList.map {
                    FileModel(
                        it.path,
                        it.name,
                        it.nameWithoutExtension,
                        getConvertedFileSize(it),
                        it.isDirectory,
                        dateFormat.format(it.lastModified()),
                        it.extension,
                        ("${it.listFiles()?.size} ${context.getString(R.string.file_count)}"),
                        getPermissions(it),
                        it.isHidden,
                        false,
                    )
                }
            } ?: let {
                // if it is null, it is most likely to be in a root directory
                val rootHandler = RootHandler()
                if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                    rootHandler.getFileList(path).filter { it.first() != '.' || showHidden }.map {
                        FileModel(
                            "$path/$it",
                            it,
                            it,
                            "",
                            it.last() == FOLDER_SUFFIX,
                            "",
                            "",
                            "",
                            MountOption.OTHER,
                            it.first() == HIDDEN_FILE_PREFIX,
                            true,
                        )
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

    private fun Double.round(): Double = "%.2f".format(this).toDouble()

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
        return 0.0
    }

    fun manageMultipleSelectionList(
        file: FileModel,
        multipleSelection: MutableList<FileModel>,
    ): MutableList<FileModel> {
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

    private fun getDocumentFile(
        file: File,
        isDirectory: Boolean,
    ): DocumentFile? {
        logd("getDocumentFile")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)
        val getExtSdCardBaseFolder: String
        try {
            getExtSdCardBaseFolder = StoragePaths.getStorageDirectories().elementAt(1)
        } catch (err: IndexOutOfBoundsException) {
            loge("getDocumentFile $err")
            return null
        }
        var originalDirectory = false
        var relativePathOfFile: String? = null
        try {
            val fullPath = file.canonicalPath
            if (getExtSdCardBaseFolder != fullPath) {
                relativePathOfFile =
                    fullPath.substring(getExtSdCardBaseFolder.length + 1)
            } else {
                originalDirectory = true
            }
        } catch (err: IOException) {
            loge("getDocumentFile $err")
            return null
        } catch (err: Exception) {
            originalDirectory = true
        }

        val extSdCardChosenUri = storageDirectoryPreferenceProvider.getChosenUri()
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
                err.printStackTrace()
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
        logi("Attempt to rename " + selectedDirectory.path + " to " + newFileName)
        if (selectedDirectory.name == newFileName) cancel()

        if (isRootDirectory(selectedDirectory.path)) {
            // do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
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
            if (!File("$dirName/$newFileName").exists()) {
                cancel()
            }
        }
    }

    suspend fun delete(selectedDirectories: List<FileModel>) =
        withContext(Dispatchers.IO) {
            if (isRootDirectory(selectedDirectories.first().path)) {
                // do it with root permissions
                if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                    rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                    val isSuccess = rootHandler.delete(selectedDirectories)
                    rootHandler.remountRootDirAs(MountOption.READ.option)
                    if (!isSuccess) cancel()
                } else {
                    cancel()
                }
            } else {
                for (i in selectedDirectories.indices) {
                    logi("Attempt to delete " + selectedDirectories[i].path)
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
            for (i in documentFile.listFiles().size - 1 downTo 0) {
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
        logi("Attempt to create folder: $path $folderName")

        if (isRootDirectory(path)) {
            // do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.createFolder(path, folderName)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            } else {
                cancel()
            }
        } else {
            if (!File("$path/$folderName").exists()) {
                // normal way
                // try catch wont work here. Doesn't throw IOException
                if (!File("$path/$folderName").mkdir()) {
                    // if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createDirectory(folderName)
                    if (!File("$path/$folderName").exists()) {
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

    suspend fun createFile(
        path: String,
        fileName: String,
    ) = withContext(Dispatchers.IO) {
        logi("Attempt to create file: $path $fileName")

        if (isRootDirectory(path)) {
            // do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
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
            if (!File("$path/$fileName").exists()) {
                try {
                    // if wont work here. Throws IOException
                    // normal way
                    File("$path/$fileName").createNewFile()
                } catch (err: Exception) {
                    err.printStackTrace()
                    // if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createFile(
                        MIME_TYPE_ALL,
                        fileName,
                    )
                    if (!File("$path/$fileName").exists()) {
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
        if (isRootDirectory(copyOrMoveDestination)) {
            // do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
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
                            sourceFiles[i], copyOrMoveDestination + File.separator + sourceFile.name,
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
                    while (fileInputStream.read(byteArray).also { bytesRead = it } != -1) {
                        outputStream?.write(byteArray, 0, bytesRead)
                    }
                } catch (err: Exception) {
                    loge("copyToExtCard $err")
                    isSuccess = false
                } finally {
                    try {
                        fileInputStream.close()
                        outputStream?.close()
                    } catch (err: Exception) {
                        loge("copyToExtCard $err")
                        isSuccess = false
                    }
                }
            } catch (err: Exception) {
                loge("copyToExtCard $err")
                isSuccess = false
            } finally {
                try {
                    fileInputStream?.close()
                    outputStream?.close()
                } catch (err: Exception) {
                    loge("copyToExtCard $err")
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
        if (isRootDirectory(copyOrMoveDestination)) {
            // do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
                val isSuccess = rootHandler.moveFile(copyOrMoveSources, copyOrMoveDestination)
                rootHandler.remountRootDirAs(MountOption.READ.option)
                if (!isSuccess) cancel()
            } else {
                cancel()
            }
        } else {
            for (i in copyOrMoveSources.indices) {
                logi("Attempt to move: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
            }
            try {
                copyFile(copyOrMoveSources, copyOrMoveDestination)
            } catch (err: CancellationException) {
                cancel()
            }
            try {
                delete(copyOrMoveSources)
            } catch (err: CancellationException) {
                cancel()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel> =
        withContext(Dispatchers.IO) {
            val fileList = mutableListOf<File>()
            try {
                val storagePaths = StoragePaths.getStorageDirectories()
                for (storagePath in storagePaths) {
                    if (storagePath != File.separator) {
                        fileList.addAll(
                            getSubSearchedFiles(
                                File(storagePath),
                                searchQuery,
                            ),
                        )
                    }
                }
                return@withContext fileList.filter { file ->
                    searchQuery.lowercase().toRegex()
                        .containsMatchIn(file.nameWithoutExtension.lowercase())
                }.map {
                    FileModel(
                        it.path,
                        it.name,
                        it.nameWithoutExtension,
                        getConvertedFileSize(it),
                        it.isDirectory,
                        dateFormat.format(it.lastModified()),
                        it.extension,
                        ("${it.listFiles()?.size} ${context.getString(R.string.file_count)}"),
                        getPermissions(it),
                        it.isHidden,
                    )
                }
            } catch (err: java.lang.Exception) {
                loge("getSearchedDeviceFiles $err")
            }
            return@withContext emptyList<FileModel>()
        }

    private fun getSubSearchedFiles(
        directory: File,
        searchQuery: String,
        res: MutableSet<File> = mutableSetOf(),
    ): Set<File> {
        // Depth first search algorithm
        directory.listFiles()?.let { fileList ->
            for (file in fileList.toSet()) {
                if (file.isDirectory) {
                    getSubSearchedFiles(file, searchQuery, res)
                } else {
                    res.add(file)
                }
                res.addAll(directory.listFiles()!!.toSet())
            }
        }
        return res
    }

    suspend fun compressFiles(
        multipleSelection: MutableList<FileModel>,
        compressedFileNameWithExtension: String,
    ) = withContext(Dispatchers.IO) {
        val parentPath: String = File(multipleSelection.first().path).parent ?: ""
        val archiveType: String =
            compressedFileNameWithExtension.substring(
                compressedFileNameWithExtension.lastIndexOf(FILE_EXTENSION_SEPARATOR),
            ).drop(1)

        if (!FileCompressionHandler().compress(
                "$parentPath/$compressedFileNameWithExtension",
                multipleSelection,
                archiveType,
            )
        ) {
            cancel()
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
                    "$parentPath/$extractedFolderName",
                )
            ) {
                cancel()
            }
        }
}
