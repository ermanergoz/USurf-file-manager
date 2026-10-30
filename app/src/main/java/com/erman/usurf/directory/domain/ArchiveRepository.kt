package com.erman.usurf.directory.domain

import com.erman.usurf.directory.model.FileModel

/** The archives the folder screen makes and opens. Every function runs off the main thread and throws FileOperationException. */
interface ArchiveRepository {
    /** Compresses [multipleSelection] into an archive named [compressedFileNameWithExtension] beside it. */
    suspend fun compressFiles(multipleSelection: List<FileModel>, compressedFileNameWithExtension: String)

    /** Extracts the archive [selectedDirectory] into a new folder beside it. */
    suspend fun extractFiles(selectedDirectory: FileModel)
}
