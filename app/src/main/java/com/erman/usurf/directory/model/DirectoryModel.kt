package com.erman.usurf.directory.model

import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.storage.domain.needsRootToWrite
import com.erman.usurf.storage.domain.volumeAmong
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val EXTRACTED_FOLDER_NAME_SUFFIX = "_extracted"
private const val OFFSET_START = 0
private const val TEMP_FOLDER_NAME = "usurfTemp"

class DirectoryModel(
    private val directoryListHandler: DirectoryListHandler,
    private val preferencesRepository: PreferencesRepository,
    private val safDocumentAccess: SafDocumentAccess,
    private val rootHandler: RootHandler,
    private val storagePathsProvider: StoragePathsProvider,
    private val fileCopier: FileCopier,
    private val fileCompressionHandler: FileCompressionHandler
) {
    suspend fun getFileModelsFromDirectory(path: String): List<FileModel> = directoryListHandler.getFileModelsFromDirectory(path)

    fun manageMultipleSelectionList(file: FileModel, multipleSelection: List<FileModel>): List<FileModel> = if (file.isSelected) {
        multipleSelection.filter { it.path != file.path }
    } else {
        multipleSelection + file.copy(isSelected = true)
    }

    // Root is used only for folders the app cannot write itself outside every storage directory, decided from the
    // path alone, so nothing is written to find out and storage is never searched.
    private fun needsRoot(folder: String): Boolean = storagePathsProvider.needsRootToWrite(folder) { File(it).canWrite() }

    private fun parentOf(path: String): String = File(path).parent ?: File.separator

    private fun hasRootAccess(): Boolean = preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()

    // The root file system is mounted read-only again however the action ends, a shell that dropped it included.
    private fun executeWithRootPermissions(action: () -> Boolean) {
        rootHandler.remountRootDirAs(MountOption.READ_WRITE)
        val isSuccess =
            try {
                action()
            } finally {
                rootHandler.remountRootDirAs(MountOption.READ)
            }
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
        if (isInvalid) throw FileOperationException("Invalid file name: $name", OperationError.InvalidName)
    }

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        requireValidName(newFileName)
        if (selectedDirectory.name == newFileName) {
            throw FileOperationException("New name matches the current name", OperationError.NameUnchanged)
        }
        requireFreeName(File(selectedDirectory.path).parentFile, selectedDirectory.name, newFileName)
        if (needsRoot(parentOf(selectedDirectory.path))) {
            executeAsRoot { rootHandler.renameFile(selectedDirectory, newFileName) }
        } else {
            renameNonRoot(selectedDirectory, newFileName)
        }
    }

    // A rename onto a taken name would replace that item (rename(2) and mv overwrite), so it is refused first.
    // On case-insensitive storage a rename that only changes case finds the item itself under the new name,
    // so then only an entry spelled exactly like the new name counts.
    private fun requireFreeName(directory: File?, currentName: String, newName: String) {
        if (directory == null || !File(directory, newName).exists()) return
        val isCaseOnlyChange = newName.equals(currentName, ignoreCase = true)
        if (!isCaseOnlyChange || directory.list()?.contains(newName) == true) {
            throw FileOperationException("An item with this name already exists", OperationError.AlreadyExists)
        }
    }

    private fun renameNonRoot(selectedDirectory: FileModel, newFileName: String) {
        val dirName = File(selectedDirectory.path).parent
        val prev = File(dirName, selectedDirectory.name)
        val new = File(dirName, newFileName)
        val isRenamed =
            prev.renameTo(new) ||
                safDocumentAccess.existingDocumentFor(prev)?.renameTo(newFileName) == true
        if (!isRenamed || !new.exists()) throw FileOperationException("Rename failed")
    }

    suspend fun delete(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        if (selectedDirectories.isEmpty()) return@withContext
        if (selectedDirectories.any { needsRoot(parentOf(it.path)) }) {
            executeAsRoot { rootHandler.delete(selectedDirectories) }
        } else {
            selectedDirectories.forEach { deleteFileNonRoot(it) }
        }
    }

    private fun deleteFileNonRoot(fileModel: FileModel) {
        val file = File(fileModel.path)
        val isSuccess = if (fileModel.isDirectory) file.deleteRecursively() else file.delete()
        if (isSuccess) return
        val document = safDocumentAccess.existingDocumentFor(file)
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
        if (needsRoot(path)) {
            executeAsRoot { rootHandler.createFolder(path, folderName) }
        } else {
            createFolderNonRoot(path, folderName)
        }
    }

    private fun createFolderNonRoot(path: String, folderName: String) {
        val folderPath = "$path${File.separator}$folderName"
        if (File(folderPath).exists()) throw FileOperationException("A folder with this name already exists", OperationError.AlreadyExists)
        if (!File(folderPath).mkdir()) {
            safDocumentAccess.documentFor(File(path), File(path).isDirectory)?.createDirectory(folderName)
            if (!File(folderPath).exists()) throw FileOperationException("Folder creation failed")
        }
    }

    suspend fun createFile(path: String, fileName: String) = withContext(Dispatchers.IO) {
        requireValidName(fileName)
        if (needsRoot(path)) {
            executeAsRoot { rootHandler.createFile(path, fileName) }
        } else {
            createFileNonRoot(path, fileName)
        }
    }

    private fun createFileNonRoot(path: String, fileName: String) {
        val filePath = "$path${File.separator}$fileName"
        if (File(filePath).exists()) throw FileOperationException("A file with this name already exists", OperationError.AlreadyExists)
        try {
            File(filePath).createNewFile()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            safDocumentAccess.documentFor(File(path), File(path).isDirectory)?.createFile(MIME_TYPE_ALL, fileName)
            if (!File(filePath).exists()) throw FileOperationException("File creation failed")
        }
    }

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (needsRoot(copyOrMoveDestination) || isSourceInRoot) {
            executeAsRoot { rootHandler.copyFile(copyOrMoveSources, copyOrMoveDestination) }
        } else {
            copyOrMoveSources.forEach { fileCopier.copy(File(it.path), File(copyOrMoveDestination)) }
        }
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        if (copyOrMoveSources.isEmpty()) return@withContext
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (needsRoot(copyOrMoveDestination) || isSourceInRoot) {
            executeAsRoot { rootHandler.moveFile(copyOrMoveSources, copyOrMoveDestination) }
        } else {
            moveFileNonRoot(copyOrMoveSources, copyOrMoveDestination)
        }
    }

    // A move on one volume is a rename. Otherwise every source is copied and its copy read back before any
    // source is deleted; when one copy fails or does not match its source, the copies already made are taken
    // back and every source stays. Taken names are refused first, so neither a rename nor a copy replaces anything.
    private suspend fun moveFileNonRoot(sources: List<FileModel>, destination: String) {
        val destinationFolder = File(destination)
        if (sources.any { File(destinationFolder, it.name).exists() }) {
            throw FileOperationException("Destination already contains an item with this name", OperationError.AlreadyExists)
        }
        val volumes = storagePathsProvider.getStorageDirectories()
        val destinationVolume = volumeAmong(volumes, destinationFolder.path)
        val copiedSources = mutableListOf<FileModel>()
        val copies = mutableListOf<CopiedItem>()
        try {
            sources.forEach { source ->
                val sourceFile = File(source.path)
                val isOnSameVolume = volumeAmong(volumes, sourceFile.path) == destinationVolume
                if (!isOnSameVolume || !sourceFile.renameTo(File(destinationFolder, source.name))) {
                    copies += copyAndCheck(sourceFile, destinationFolder)
                    copiedSources += source
                }
            }
        } catch (err: FileOperationException) {
            copies.forEach { it.remove() }
            throw FileOperationException("Move failed; nothing was deleted", OperationError.MoveFailedNothingDeleted, err)
        }
        if (copiedSources.isNotEmpty()) delete(copiedSources)
    }

    private fun copyAndCheck(source: File, destinationFolder: File): CopiedItem {
        val expected = EntryShape.of(source)
        val copy = fileCopier.copy(source, destinationFolder)
        if (expected == null || expected != copy.shape()) {
            copy.remove()
            throw FileOperationException("The copy of ${source.name} does not match it")
        }
        return copy
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
        if (File(outputPath).exists()) {
            throw FileOperationException("An archive with this name already exists", OperationError.AlreadyExists)
        }
        val isCompressed =
            fileCompressionHandler.compress(outputPath, multipleSelection, archiveType)
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
            throw FileOperationException("Archive name needs a valid extension", OperationError.ArchiveExtensionMissing)
        }
    }

    private fun extractArchiveType(fileNameWithExtension: String): String = fileNameWithExtension.substring(
        fileNameWithExtension.lastIndexOf(FILE_EXTENSION_SEPARATOR)
    ).drop(SUFFIX_LENGTH)

    private suspend fun compressViaCache(sources: List<FileModel>, compressedFileName: String, archiveType: String, parentPath: String) {
        val cachedFolderDirectory =
            fileCompressionHandler.cacheDirectory.absolutePath + File.separator + TEMP_FOLDER_NAME
        val compressedFileDirectory = "$cachedFolderDirectory${File.separator}$compressedFileName"
        createFolder(fileCompressionHandler.cacheDirectory.absolutePath, TEMP_FOLDER_NAME)
        try {
            copyFile(sources, cachedFolderDirectory)
            val cachedFiles = getFileModelsFromDirectory(cachedFolderDirectory).toMutableList()
            val isCompressed =
                fileCompressionHandler.compress(compressedFileDirectory, cachedFiles, archiveType)
            if (isCompressed) {
                moveCompressedFileToParent(compressedFileDirectory, compressedFileName, parentPath)
            } else {
                throw FileOperationException("Compression failed", OperationError.CompressionFailed)
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
        // createFolder refuses a folder that already exists, so from here on the folder is one this extraction made.
        createFolder(parentPath, extractedFolderName)
        var isDone = false
        try {
            val isExtracted =
                fileCompressionHandler.extract(selectedDirectory.path, extractionDestination)
            if (!isExtracted) {
                extractViaCache(selectedDirectory, extractedFolderName, extractionDestination)
            }
            isDone = true
        } finally {
            if (!isDone) removeIfEmpty(File(extractionDestination))
        }
    }

    // A folder that received part of the files stays, so the user can see what was extracted.
    private fun removeIfEmpty(folder: File) {
        if (folder.list()?.isEmpty() == true && !folder.delete()) {
            safDocumentAccess.existingDocumentFor(folder)?.delete()
        }
    }

    private suspend fun extractViaCache(selectedDirectory: FileModel, extractedFolderName: String, extractionDestination: String) {
        val cachedFolderDirectory =
            fileCompressionHandler.cacheDirectory.absolutePath + File.separator + TEMP_FOLDER_NAME
        val cachedCompressedFilePath =
            cachedFolderDirectory + File.separator + selectedDirectory.name
        val cachedExtractedFolderPath = cachedFolderDirectory + File.separator + extractedFolderName
        createFolder(fileCompressionHandler.cacheDirectory.absolutePath, TEMP_FOLDER_NAME)
        try {
            copyFile(listOf(selectedDirectory), cachedFolderDirectory)
            createFolder(cachedFolderDirectory, extractedFolderName)
            val isExtracted =
                fileCompressionHandler.extract(cachedCompressedFilePath, cachedExtractedFolderPath)
            if (isExtracted) {
                moveExtractedFilesToDestination(cachedExtractedFolderPath, extractionDestination)
            } else {
                throw FileOperationException("Extraction failed", OperationError.ExtractionFailed)
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
