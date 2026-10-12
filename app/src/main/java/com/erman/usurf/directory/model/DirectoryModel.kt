package com.erman.usurf.directory.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DUMMY_FILE_NAME = "usurfIsRootCheck.txt"
private const val DUMMY_FILE_NAME_WO_EXTENSION = "usurfIsRootCheck"
private const val EXTRACTED_FOLDER_NAME_SUFFIX = "_extracted"
private const val BUFFER_SIZE = 131072 // Ideal for most devices also considering the old devices
private const val END_OF_STREAM = -1
private const val OFFSET_START = 0
private const val TEMP_FOLDER_NAME = "usurfTemp"

class DirectoryModel(
    private val directoryListHandler: DirectoryListHandler,
    private val preferencesRepository: PreferencesRepository,
    private val safDocumentAccess: SafDocumentAccess,
    private val rootHandler: RootHandler,
    private val context: Context,
    private val storagePathsProvider: StoragePathsProvider
) {
    suspend fun getFileModelsFromDirectory(path: String): List<FileModel> = directoryListHandler.getFileModelsFromDirectory(path)

    fun manageMultipleSelectionList(file: FileModel, multipleSelection: List<FileModel>): List<FileModel> = if (file.isSelected) {
        multipleSelection.filter { it.path != file.path }
    } else {
        multipleSelection + file.copy(isSelected = true)
    }

    private suspend fun isRootDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        val dummyFilePath = getParentDirectoryPath(path)
        val dummyFile = File(dummyFilePath + File.separator + DUMMY_FILE_NAME)
        createDummyFile(dummyFilePath)
        val isRoot: Boolean = !dummyFile.exists()
        cleanUpDummyFile(isRoot, dummyFile)
        return@withContext isRoot
    }

    private fun getParentDirectoryPath(path: String): String {
        val pathFile = File(path)
        return if (pathFile.isDirectory) pathFile.path else pathFile.parent.orEmpty()
    }

    private fun createDummyFile(parentPath: String) {
        try {
            File(parentPath + File.separator + DUMMY_FILE_NAME).createNewFile()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            createDummyFileWithSaf(parentPath)
        }
    }

    private fun createDummyFileWithSaf(parentPath: String) {
        safDocumentAccess.documentFor(File(parentPath), File(parentPath).isDirectory)?.createFile(
            MIME_TYPE_ALL,
            DUMMY_FILE_NAME
        )
    }

    private suspend fun cleanUpDummyFile(isRoot: Boolean, dummyFile: File) {
        if (!isRoot) {
            deleteDummyFile(dummyFile)
        } else {
            deleteDummyFileFromDeviceSearch()
        }
    }

    private fun deleteDummyFile(dummyFile: File) {
        if (!dummyFile.delete()) {
            deleteDummyFileWithSaf(dummyFile)
        }
    }

    private fun deleteDummyFileWithSaf(dummyFile: File) {
        val documentFileToDelete = safDocumentAccess.documentFor(dummyFile, dummyFile.isDirectory)
        documentFileToDelete?.let { deleteFolderRecursively(it) }
    }

    private suspend fun deleteDummyFileFromDeviceSearch() {
        val toDelete = getSearchedDeviceFiles(DUMMY_FILE_NAME_WO_EXTENSION)
        if (toDelete.isNotEmpty()) delete(toDelete)
    }

    private fun hasRootAccess(): Boolean = preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()

    private fun executeWithRootPermissions(action: () -> Boolean) {
        rootHandler.remountRootDirAs(MountOption.READ_WRITE.option)
        val isSuccess = action()
        rootHandler.remountRootDirAs(MountOption.READ.option)
        if (!isSuccess) throw FileOperationException("Root operation failed")
    }

    private fun executeAsRoot(action: () -> Boolean) {
        if (!hasRootAccess()) throw FileOperationException("Root access is not available")
        executeWithRootPermissions(action)
    }

    private fun requireValidName(name: String) {
        val isInvalid =
            name.isBlank() ||
                name == "." ||
                name == ".." ||
                name.contains(File.separatorChar)
        if (isInvalid) throw FileOperationException("Invalid file name: $name")
    }

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        requireValidName(newFileName)
        if (selectedDirectory.name == newFileName) throw FileOperationException("New name matches the current name")
        if (isRootDirectory(selectedDirectory.path)) {
            executeAsRoot { rootHandler.renameFile(selectedDirectory, newFileName) }
        } else {
            renameNonRoot(selectedDirectory, newFileName)
        }
    }

    private fun renameNonRoot(selectedDirectory: FileModel, newFileName: String) {
        val dirName = File(selectedDirectory.path).parent
        val prev = File(dirName, selectedDirectory.name)
        val new = File(dirName, newFileName)
        val isRenamed =
            prev.renameTo(new) ||
                safDocumentAccess.documentFor(prev, selectedDirectory.isDirectory)?.renameTo(newFileName) == true
        if (!isRenamed || !new.exists()) throw FileOperationException("Rename failed")
    }

    suspend fun delete(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        if (selectedDirectories.isEmpty()) return@withContext
        if (isRootDirectory(selectedDirectories.first().path)) {
            executeAsRoot { rootHandler.delete(selectedDirectories) }
        } else {
            selectedDirectories.forEach { deleteFileNonRoot(it) }
        }
    }

    private fun deleteFileNonRoot(fileModel: FileModel) {
        val file = File(fileModel.path)
        val isSuccess = if (fileModel.isDirectory) file.deleteRecursively() else file.delete()
        if (isSuccess) return
        val document = safDocumentAccess.documentFor(file, fileModel.isDirectory)
        if (document == null || !deleteFolderRecursively(document)) throw FileOperationException("Delete failed")
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

    suspend fun createFolder(path: String, folderName: String) = withContext(Dispatchers.IO) {
        requireValidName(folderName)
        if (isRootDirectory(path)) {
            executeAsRoot { rootHandler.createFolder(path, folderName) }
        } else {
            createFolderNonRoot(path, folderName)
        }
    }

    private fun createFolderNonRoot(path: String, folderName: String) {
        val folderPath = "$path${File.separator}$folderName"
        if (File(folderPath).exists()) throw FileOperationException("A folder with this name already exists")
        if (!File(folderPath).mkdir()) {
            safDocumentAccess.documentFor(File(path), File(path).isDirectory)?.createDirectory(folderName)
            if (!File(folderPath).exists()) throw FileOperationException("Folder creation failed")
        }
    }

    suspend fun createFile(path: String, fileName: String) = withContext(Dispatchers.IO) {
        requireValidName(fileName)
        if (isRootDirectory(path)) {
            executeAsRoot { rootHandler.createFile(path, fileName) }
        } else {
            createFileNonRoot(path, fileName)
        }
    }

    private fun createFileNonRoot(path: String, fileName: String) {
        val filePath = "$path${File.separator}$fileName"
        if (File(filePath).exists()) throw FileOperationException("A file with this name already exists")
        try {
            File(filePath).createNewFile()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            safDocumentAccess.documentFor(File(path), File(path).isDirectory)?.createFile(MIME_TYPE_ALL, fileName)
            if (!File(filePath).exists()) throw FileOperationException("File creation failed")
        }
    }

    private fun doesFileExist(fileModel: FileModel, copyOrMoveDestination: String): Boolean =
        File(copyOrMoveDestination).listFiles()?.any { it.name == fileModel.name } ?: false

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (isRootDirectory(copyOrMoveDestination) || isSourceInRoot) {
            executeAsRoot { rootHandler.copyFile(copyOrMoveSources, copyOrMoveDestination) }
        } else {
            copyOrMoveSources.forEach { copySingleFileNonRoot(it, copyOrMoveDestination) }
        }
    }

    private fun copySingleFileNonRoot(source: FileModel, destination: String) {
        if (doesFileExist(source, destination)) {
            throw FileOperationException("Destination already contains an item with this name")
        }
        if (source.isDirectory) {
            copyDirectoryNonRoot(source, destination)
        } else {
            copySingleFile(source, destination)
        }
    }

    private fun copyDirectoryNonRoot(source: FileModel, destination: String) {
        val isCopied =
            try {
                File(source.path).copyRecursively(File(destination + File.separator + source.name))
            } catch (err: Exception) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
                false
            }
        if (!isCopied && !copyToExtCard(File(source.path), destination)) throw FileOperationException("Copy failed")
    }

    private fun copySingleFile(source: FileModel, destination: String) {
        val isCopied =
            try {
                File(source.path).copyTo(File(destination + File.separator + source.name))
                true
            } catch (err: IOException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
                false
            }
        if (!isCopied && !copyToExtCard(File(source.path), destination)) throw FileOperationException("Copy failed")
    }

    private fun copyToExtCard(sourceFile: File, destination: String): Boolean = if (sourceFile.isDirectory) {
        copyDirectoryToExtCard(sourceFile, destination)
    } else {
        copyFileToExtCard(sourceFile, destination)
    }

    private fun copyDirectoryToExtCard(sourceFile: File, destination: String): Boolean {
        safDocumentAccess.documentFor(File(destination), File(destination).isDirectory)
            ?.createDirectory(sourceFile.name)
        var isSuccess = true
        sourceFile.listFiles()?.forEach { subFile ->
            isSuccess = copyToExtCard(subFile, destination + File.separator + sourceFile.name)
        }
        return isSuccess
    }

    private fun copyFileToExtCard(sourceFile: File, destination: String): Boolean {
        val documentFile =
            safDocumentAccess.documentFor(File(destination), File(destination).isDirectory)
                ?.createFile(sourceFile.extension, sourceFile.name)
                ?: return false
        return writeFileToDocumentFile(sourceFile, documentFile)
    }

    private fun writeFileToDocumentFile(sourceFile: File, documentFile: DocumentFile): Boolean {
        var fileInputStream: FileInputStream? = null
        var outputStream: OutputStream? = null
        return try {
            fileInputStream = FileInputStream(sourceFile)
            outputStream = context.contentResolver.openOutputStream(documentFile.uri)
            transferStream(fileInputStream, outputStream)
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            false
        } finally {
            closeStreams(fileInputStream, outputStream)
        }
    }

    private fun transferStream(inputStream: FileInputStream, outputStream: OutputStream?): Boolean {
        val byteArray = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        return try {
            while (inputStream.read(byteArray).also { bytesRead = it } != END_OF_STREAM) {
                outputStream?.write(byteArray, OFFSET_START, bytesRead)
            }
            true
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            false
        }
    }

    private fun closeStreams(fileInputStream: FileInputStream?, outputStream: OutputStream?) {
        try {
            fileInputStream?.close()
            outputStream?.close()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (isRootDirectory(copyOrMoveDestination) || isSourceInRoot) {
            executeAsRoot { rootHandler.moveFile(copyOrMoveSources, copyOrMoveDestination) }
        } else {
            moveFileNonRoot(copyOrMoveSources, copyOrMoveDestination)
        }
    }

    private suspend fun moveFileNonRoot(sources: List<FileModel>, destination: String) {
        copyFile(sources, destination)
        delete(sources)
    }

    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel> = withContext(Dispatchers.IO) {
        val matchedFiles = mutableListOf<File>()
        try {
            val query = searchQuery.lowercase()
            val storagePaths = storagePathsProvider.getStorageDirectories()
            for (storagePath in storagePaths) {
                if (storagePath != File.separator) {
                    searchRecursively(File(storagePath), query, matchedFiles)
                }
            }
            return@withContext directoryListHandler.mapFilesToFileModels(matchedFiles)
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
        return@withContext emptyList<FileModel>()
    }

    private fun searchRecursively(directory: File, lowercaseQuery: String, matches: MutableList<File>) {
        // Depth first search that keeps only the matching files instead of every file on the device
        directory.listFiles()?.forEach { file ->
            if (file.nameWithoutExtension.lowercase().contains(lowercaseQuery)) {
                matches.add(file)
            }
            if (file.isDirectory) {
                searchRecursively(file, lowercaseQuery, matches)
            }
        }
    }

    suspend fun compressFiles(multipleSelection: List<FileModel>, compressedFileNameWithExtension: String) = withContext(Dispatchers.IO) {
        if (multipleSelection.isEmpty()) return@withContext
        requireValidName(compressedFileNameWithExtension)
        requireArchiveExtension(compressedFileNameWithExtension)
        val parentPath: String = File(multipleSelection.first().path).parent ?: ""
        val archiveType: String = extractArchiveType(compressedFileNameWithExtension)
        val outputPath = "$parentPath${File.separator}$compressedFileNameWithExtension"
        if (File(outputPath).exists()) throw FileOperationException("An archive with this name already exists")
        val isCompressed =
            FileCompressionHandler().compress(outputPath, multipleSelection, archiveType)
        if (!isCompressed) {
            compressViaCache(
                multipleSelection,
                compressedFileNameWithExtension,
                archiveType,
                parentPath
            )
        }
    }

    private fun requireArchiveExtension(name: String) {
        val separatorIndex = name.lastIndexOf(FILE_EXTENSION_SEPARATOR)
        if (separatorIndex !in 1 until name.length - 1) {
            throw FileOperationException("Archive name needs a valid extension")
        }
    }

    private fun extractArchiveType(fileNameWithExtension: String): String = fileNameWithExtension.substring(
        fileNameWithExtension.lastIndexOf(FILE_EXTENSION_SEPARATOR)
    ).drop(SUFFIX_LENGTH)

    private suspend fun compressViaCache(sources: List<FileModel>, compressedFileName: String, archiveType: String, parentPath: String) {
        val cachedFolderDirectory =
            context.cacheDir.absolutePath + File.separator + TEMP_FOLDER_NAME
        val compressedFileDirectory = "$cachedFolderDirectory${File.separator}$compressedFileName"
        createFolder(context.cacheDir.absolutePath, TEMP_FOLDER_NAME)
        try {
            copyFile(sources, cachedFolderDirectory)
            val cachedFiles = getFileModelsFromDirectory(cachedFolderDirectory).toMutableList()
            val isCompressed =
                FileCompressionHandler().compress(compressedFileDirectory, cachedFiles, archiveType)
            if (isCompressed) {
                moveCompressedFileToParent(compressedFileDirectory, compressedFileName, parentPath)
            } else {
                throw FileOperationException("Compression failed")
            }
        } finally {
            deleteTempFolder(cachedFolderDirectory)
        }
    }

    private suspend fun moveCompressedFileToParent(compressedFilePath: String, compressedFileName: String, parentPath: String) {
        moveFile(
            listOf(
                FileModel(
                    path = compressedFilePath,
                    name = compressedFileName,
                    isDirectory = false
                )
            ),
            parentPath
        )
    }

    private suspend fun deleteTempFolder(cachedFolderDirectory: String) {
        delete(
            listOf(
                FileModel(
                    path = cachedFolderDirectory,
                    name = TEMP_FOLDER_NAME,
                    isDirectory = true
                )
            )
        )
    }

    suspend fun extractFiles(selectedDirectory: FileModel) = withContext(Dispatchers.IO) {
        val parentPath: String = File(selectedDirectory.path).parent ?: ""
        val extractedFolderName =
            selectedDirectory.nameWithoutExtension + EXTRACTED_FOLDER_NAME_SUFFIX
        val extractionDestination = "$parentPath${File.separator}$extractedFolderName"
        createFolder(parentPath, extractedFolderName)
        val isExtracted =
            FileCompressionHandler().extract(selectedDirectory.path, extractionDestination)
        if (!isExtracted) {
            extractViaCache(selectedDirectory, extractedFolderName, extractionDestination)
        }
    }

    private suspend fun extractViaCache(selectedDirectory: FileModel, extractedFolderName: String, extractionDestination: String) {
        val cachedFolderDirectory =
            context.cacheDir.absolutePath + File.separator + TEMP_FOLDER_NAME
        val cachedCompressedFilePath =
            cachedFolderDirectory + File.separator + selectedDirectory.name
        val cachedExtractedFolderPath = cachedFolderDirectory + File.separator + extractedFolderName
        createFolder(context.cacheDir.absolutePath, TEMP_FOLDER_NAME)
        try {
            copyFile(listOf(selectedDirectory), cachedFolderDirectory)
            createFolder(cachedFolderDirectory, extractedFolderName)
            val isExtracted =
                FileCompressionHandler().extract(cachedCompressedFilePath, cachedExtractedFolderPath)
            if (isExtracted) {
                moveExtractedFilesToDestination(cachedExtractedFolderPath, extractionDestination)
            } else {
                throw FileOperationException("Extraction failed")
            }
        } finally {
            deleteTempFolder(cachedFolderDirectory)
        }
    }

    private suspend fun moveExtractedFilesToDestination(cachedExtractedFolderPath: String, extractionDestination: String) {
        val extractedFiles = getFileModelsFromDirectory(cachedExtractedFolderPath).toMutableList()
        moveFile(extractedFiles, extractionDestination)
    }
}
