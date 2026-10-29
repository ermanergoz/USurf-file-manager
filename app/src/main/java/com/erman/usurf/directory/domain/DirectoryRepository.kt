package com.erman.usurf.directory.domain

import com.erman.usurf.directory.model.FileModel

/**
 * The folders and files the folder screen lists and changes. Every function runs off the main thread and throws
 * FileOperationException and nothing else.
 */
interface DirectoryRepository {
    suspend fun getFileModelsFromDirectory(path: String): List<FileModel>

    /** Every item on the storage volumes whose name holds [searchQuery], in any case. */
    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel>

    /** The folder above [path] when the app can read it; null at the top of the file system or when it cannot be read. */
    suspend fun readableParentOf(path: String): String?

    /** The items at [paths], each a folder or a file as it is on the device now. */
    suspend fun itemsAt(paths: List<String>): List<FileModel>

    suspend fun rename(selectedDirectory: FileModel, newFileName: String)

    /** Deletes the items at [paths], each a folder or a file as it is on the device when the delete runs. */
    suspend fun deletePaths(paths: List<String>)

    suspend fun createFolder(path: String, folderName: String)

    suspend fun createFile(path: String, fileName: String)

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String)

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String)
}
