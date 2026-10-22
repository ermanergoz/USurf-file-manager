package com.erman.usurf.directory.model

import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.storage.domain.volumeAmong
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val OFFSET_START = 0

class DirectoryModel(
    private val directoryListHandler: DirectoryListHandler,
    private val safDocumentAccess: SafDocumentAccess,
    private val rootWriter: RootWriter,
    private val storagePathsProvider: StoragePathsProvider,
    private val fileCopier: FileCopier,
    private val ioDispatcher: CoroutineDispatcher
) : FileOperations {
    override suspend fun getFileModelsFromDirectory(path: String): List<FileModel> = operationOn(ioDispatcher) {
        directoryListHandler.getFileModelsFromDirectory(path)
    }

    /** The size of the folder at [path], read only when asked for, since a listing never walks the folders it shows. */
    suspend fun getFolderSize(path: String): String = operationOn(ioDispatcher) { directoryListHandler.folderSizeText(path) }

    fun manageMultipleSelectionList(file: FileModel, multipleSelection: List<FileModel>): List<FileModel> = if (file.isSelected) {
        multipleSelection.filter { it.path != file.path }
    } else {
        multipleSelection + file.copy(isSelected = true)
    }

    private fun needsRoot(folder: String): Boolean = rootWriter.needsRoot(folder)

    private fun parentOf(path: String): String = File(path).parent ?: File.separator

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = operationOn(ioDispatcher) {
        requireValidName(newFileName)
        if (selectedDirectory.name == newFileName) {
            throw FileOperationException("New name matches the current name", OperationError.NameUnchanged)
        }
        requireFreeName(File(selectedDirectory.path).parentFile, selectedDirectory.name, newFileName)
        if (needsRoot(parentOf(selectedDirectory.path))) {
            rootWriter.write { root -> root.renameFile(selectedDirectory, newFileName) }
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

    override suspend fun delete(selectedDirectories: List<FileModel>) = operationOn(ioDispatcher) {
        if (selectedDirectories.isEmpty()) return@operationOn
        // An item listed through root goes through root, as a copy or move of one does: root may own what it holds.
        if (selectedDirectories.any { it.isInRoot || needsRoot(parentOf(it.path)) }) {
            rootWriter.write { root -> root.delete(selectedDirectories) }
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

    override suspend fun createFolder(path: String, folderName: String) = operationOn(ioDispatcher) {
        requireValidName(folderName)
        if (needsRoot(path)) {
            rootWriter.write { root -> root.createFolder(path, folderName) }
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

    suspend fun createFile(path: String, fileName: String) = operationOn(ioDispatcher) {
        requireValidName(fileName)
        if (needsRoot(path)) {
            rootWriter.write { root -> root.createFile(path, fileName) }
        } else {
            createFileNonRoot(path, fileName)
        }
    }

    private fun createFileNonRoot(path: String, fileName: String) {
        val filePath = "$path${File.separator}$fileName"
        if (File(filePath).exists()) throw FileOperationException("A file with this name already exists", OperationError.AlreadyExists)
        try {
            File(filePath).createNewFile()
        } catch (err: IOException) {
            createFileInGrantedTree(path, fileName, err)
        } catch (err: SecurityException) {
            createFileInGrantedTree(path, fileName, err)
        }
    }

    // The file API refuses the new file, as the SD card does on API 29 and lower until its tree is granted, so it is
    // made through the granted tree; the refusal stays the cause when that fails too.
    private fun createFileInGrantedTree(path: String, fileName: String, refusal: Exception) {
        loge(refusal.localizedMessage ?: UNKNOWN_ERROR)
        safDocumentAccess.documentFor(File(path), File(path).isDirectory)?.createFile(MIME_TYPE_ALL, fileName)
        if (!File("$path${File.separator}$fileName").exists()) throw FileOperationException("File creation failed", cause = refusal)
    }

    override suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = operationOn(ioDispatcher) {
        if (copyOrMoveSources.isEmpty()) return@operationOn
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (needsRoot(copyOrMoveDestination) || isSourceInRoot) {
            rootWriter.write { root -> root.copyFile(copyOrMoveSources, copyOrMoveDestination) }
        } else {
            copyOrMoveSources.forEach { fileCopier.copy(File(it.path), File(copyOrMoveDestination)) }
        }
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = operationOn(ioDispatcher) {
        if (copyOrMoveSources.isEmpty()) return@operationOn
        val isSourceInRoot = copyOrMoveSources.any { it.isInRoot }
        if (needsRoot(copyOrMoveDestination) || isSourceInRoot) {
            rootWriter.write { root -> root.moveFile(copyOrMoveSources, copyOrMoveDestination) }
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

    override suspend fun copyVerified(source: File, destinationFolder: File) {
        operationOn(ioDispatcher) {
            if (needsRoot(destinationFolder.path)) {
                val item = FileModel(path = source.path, name = source.name, isDirectory = source.isDirectory)
                rootWriter.write { root -> root.copyFile(listOf(item), destinationFolder.path) }
            } else {
                copyAndCheck(source, destinationFolder)
            }
        }
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

    // A search that fails says so instead of returning nothing, which would read as a device without a match.
    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel> = operationOn(ioDispatcher) {
        val matchedFiles = mutableListOf<File>()
        val query = searchQuery.lowercase()
        val storagePaths = storagePathsProvider.getStorageDirectories()
        for (storagePath in storagePaths) {
            if (storagePath != File.separator) {
                searchRecursively(File(storagePath), query, matchedFiles)
            }
        }
        directoryListHandler.mapFilesToFileModels(matchedFiles)
    }

    // Depth first search that keeps only the matching files instead of every file on the device. A search that was
    // cancelled, by a second search or by leaving the screen, stops at the next folder instead of walking on.
    private suspend fun searchRecursively(directory: File, lowercaseQuery: String, matches: MutableList<File>) {
        currentCoroutineContext().ensureActive()
        directory.listFiles()?.forEach { file ->
            if (file.nameWithoutExtension.lowercase().contains(lowercaseQuery)) {
                matches.add(file)
            }
            if (file.isDirectory) {
                searchRecursively(file, lowercaseQuery, matches)
            }
        }
    }
}

// A name the user typed for a new or renamed item: refused when empty, a dot entry or a path.
internal fun requireValidName(name: String) {
    val isInvalid =
        name.isBlank() ||
            name == "." ||
            name == ".." ||
            name.contains(File.separatorChar)
    if (isInvalid) throw FileOperationException("Invalid file name: $name", OperationError.InvalidName)
}
