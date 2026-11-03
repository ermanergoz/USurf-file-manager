package com.erman.usurf.directory.model

import java.io.File

/** The file work the archive operations build on; every function throws FileOperationException and nothing else. */
interface FileOperations {
    suspend fun getFileModelsFromDirectory(path: String): List<FileModel>

    suspend fun createFolder(path: String, folderName: String)

    suspend fun delete(selectedDirectories: List<FileModel>)

    /**
     * Copies [sources] into [destinationFolder], a folder of the app's own, so that every copy is the app's to read and
     * delete, a copy made through root of an item listed through root included; the sources stay as they are.
     */
    suspend fun copyOwned(sources: List<FileModel>, destinationFolder: String)

    /** Copies [source] into [destinationFolder] and reads the copy back against it; the source stays as it is. */
    suspend fun copyVerified(source: File, destinationFolder: File)
}
