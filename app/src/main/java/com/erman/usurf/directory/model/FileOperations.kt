package com.erman.usurf.directory.model

import java.io.File

/** The file work the archive operations build on; every function throws FileOperationException and nothing else. */
interface FileOperations {
    suspend fun getFileModelsFromDirectory(path: String): List<FileModel>

    suspend fun createFolder(path: String, folderName: String)

    suspend fun delete(selectedDirectories: List<FileModel>)

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String)

    /** Copies [source] into [destinationFolder] and reads the copy back against it; the source stays as it is. */
    suspend fun copyVerified(source: File, destinationFolder: File)
}
