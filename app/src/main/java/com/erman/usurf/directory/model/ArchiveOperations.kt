package com.erman.usurf.directory.model

import com.erman.usurf.directory.domain.ArchiveRepository
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher

private const val EXTRACTED_FOLDER_NAME_SUFFIX = "_extracted"
private const val TEMP_FOLDER_PREFIX = "usurfTemp"

/** Where a compression writes its archive: the selection's folder, the name asked for and the 7-Zip type it names. */
data class ArchiveTarget(val parentPath: String, val fileName: String, val type: String) {
    val path: String
        get() = "$parentPath${File.separator}$fileName"

    companion object {
        /** The target of compressing [selection] into [fileNameWithExtension]; a name with no type to tell is refused. */
        fun of(selection: List<FileModel>, fileNameWithExtension: String): ArchiveTarget {
            requireValidName(fileNameWithExtension)
            val separatorIndex = fileNameWithExtension.lastIndexOf(FILE_EXTENSION_SEPARATOR)
            if (separatorIndex !in 1 until fileNameWithExtension.length - 1) {
                throw FileOperationException("Archive name needs a valid extension", OperationError.ArchiveExtensionMissing)
            }
            return ArchiveTarget(
                parentPath = File(selection.first().path).parent ?: "",
                fileName = fileNameWithExtension,
                type = fileNameWithExtension.substring(separatorIndex).drop(SUFFIX_LENGTH)
            )
        }
    }
}

/**
 * Compresses and extracts with 7-Zip, in the selection's own folder or, where 7-Zip cannot write there, as on an SD
 * card, through the app's cache folder, placing the result with the directory model's file operations. Every public
 * function throws FileOperationException and nothing else.
 */
class ArchiveOperations(
    private val fileCompressionHandler: FileCompressionHandler,
    private val fileOperations: FileOperations,
    private val safDocumentAccess: SafDocumentAccess,
    private val ioDispatcher: CoroutineDispatcher
) : ArchiveRepository {
    // The cache folders the archive runs of this process work in now; any other one is a leftover.
    private val liveTempFolders: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    override suspend fun compressFiles(multipleSelection: List<FileModel>, compressedFileNameWithExtension: String) =
        operationOn(ioDispatcher) {
            if (multipleSelection.isEmpty()) return@operationOn
            val target = ArchiveTarget.of(multipleSelection, compressedFileNameWithExtension)
            if (File(target.path).exists()) {
                throw FileOperationException("An archive with this name already exists", OperationError.AlreadyExists)
            }
            val isCompressed = fileCompressionHandler.compress(target.path, multipleSelection, target.type)
            if (!isCompressed) compressViaCache(multipleSelection, target)
        }

    private suspend fun compressViaCache(sources: List<FileModel>, target: ArchiveTarget) =
        inCacheTempFolder(sources.any { it.isInRoot }) { tempFolder ->
            val cachedArchivePath = "${tempFolder.path}${File.separator}${target.fileName}"
            fileOperations.copyFile(sources, tempFolder.path)
            val cachedFiles = fileOperations.getFileModelsFromDirectory(tempFolder.path).toMutableList()
            val isCompressed = fileCompressionHandler.compress(cachedArchivePath, cachedFiles, target.type)
            if (isCompressed) {
                placeArchiveInParent(File(cachedArchivePath), target)
            } else {
                throw FileOperationException("Compression failed", OperationError.CompressionFailed)
            }
        }

    // The cache lies on no storage volume, so the archive reaches its folder as a copy that is read back, and the cached
    // archive goes with the temp folder. A copy the platform refuses stays a refusal, the case an SD card grant can help.
    private suspend fun placeArchiveInParent(cachedArchive: File, target: ArchiveTarget) {
        try {
            fileOperations.copyVerified(cachedArchive, File(target.parentPath))
        } catch (err: FileOperationException) {
            if (err.error != null) throw err
            throw FileOperationException("The archive could not be placed in ${target.parentPath}", cause = err)
        }
    }

    // Every run through the cache works in a folder of its own, made inside the try so a failed start is cleaned up
    // too, and deleted however the run ends. A folder a killed run left behind is deleted first, so it never blocks or
    // fills the cache; a folder another run of this process still works in is kept. Root copies items listed through
    // root into the cache with root kept as their owner, so the folder of such a run, [holdsRootCopies], is deleted
    // through root.
    private suspend fun <T> inCacheTempFolder(holdsRootCopies: Boolean, work: suspend (File) -> T): T {
        deleteLeftoverTempFolders()
        val tempFolder = File(fileCompressionHandler.cacheDirectory, "$TEMP_FOLDER_PREFIX-${System.nanoTime()}")
        liveTempFolders += tempFolder.name
        try {
            fileOperations.createFolder(fileCompressionHandler.cacheDirectory.absolutePath, tempFolder.name)
            return work(tempFolder)
        } finally {
            try {
                deleteTempFolder(tempFolder, holdsRootCopies)
            } finally {
                liveTempFolders -= tempFolder.name
            }
        }
    }

    // A leftover that cannot be deleted is logged and left: it no longer blocks a run, which works in a folder of its own.
    private suspend fun deleteLeftoverTempFolders() {
        val leftovers =
            fileCompressionHandler.cacheDirectory.listFiles().orEmpty()
                .filter { it.name.startsWith(TEMP_FOLDER_PREFIX) && it.name !in liveTempFolders }
        leftovers.forEach { leftover ->
            try {
                deleteTempFolder(leftover, holdsRootCopies = false)
            } catch (err: FileOperationException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            }
        }
    }

    private suspend fun deleteTempFolder(tempFolder: File, holdsRootCopies: Boolean) {
        val folder = FileModel(path = tempFolder.path, name = tempFolder.name, isDirectory = true, isInRoot = holdsRootCopies)
        fileOperations.delete(listOf(folder))
    }

    override suspend fun extractFiles(selectedDirectory: FileModel) = operationOn(ioDispatcher) {
        val parentPath: String = File(selectedDirectory.path).parent ?: ""
        val extractedFolderName = freeExtractionFolderName(parentPath, selectedDirectory.nameWithoutExtension)
        val extractionDestination = "$parentPath${File.separator}$extractedFolderName"
        // createFolder refuses a folder that already exists, so from here on the folder is one this extraction made.
        fileOperations.createFolder(parentPath, extractedFolderName)
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

    // An archive extracted again goes into a new folder, "<name>_extracted (2)" and on, so no earlier result is mixed in.
    private fun freeExtractionFolderName(parentPath: String, archiveName: String): String {
        val baseName = archiveName + EXTRACTED_FOLDER_NAME_SUFFIX
        return generateSequence(1) { it + 1 }
            .map { number -> if (number == 1) baseName else "$baseName ($number)" }
            .first { name -> !File(parentPath, name).exists() }
    }

    // A folder that received part of the files stays, so the user can see what was extracted.
    private fun removeIfEmpty(folder: File) {
        if (folder.list()?.isEmpty() == true && !folder.delete()) {
            safDocumentAccess.existingDocumentFor(folder)?.delete()
        }
    }

    private suspend fun extractViaCache(selectedDirectory: FileModel, extractedFolderName: String, extractionDestination: String) =
        inCacheTempFolder(selectedDirectory.isInRoot) { tempFolder ->
            val cachedCompressedFilePath = tempFolder.path + File.separator + selectedDirectory.name
            val cachedExtractedFolderPath = tempFolder.path + File.separator + extractedFolderName
            fileOperations.copyFile(listOf(selectedDirectory), tempFolder.path)
            fileOperations.createFolder(tempFolder.path, extractedFolderName)
            val isExtracted =
                fileCompressionHandler.extract(cachedCompressedFilePath, cachedExtractedFolderPath)
            if (isExtracted) {
                copyExtractedFilesToDestination(cachedExtractedFolderPath, extractionDestination)
            } else {
                throw FileOperationException("Extraction failed", OperationError.ExtractionFailed)
            }
        }

    // Every extracted entry, hidden ones included, reaches the destination as a copy that is read back.
    private suspend fun copyExtractedFilesToDestination(cachedExtractedFolderPath: String, extractionDestination: String) {
        val destination = File(extractionDestination)
        File(cachedExtractedFolderPath).listFiles().orEmpty().forEach { fileOperations.copyVerified(it, destination) }
    }
}
