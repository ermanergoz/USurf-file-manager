package com.erman.usurf.directory.domain

/** How much a folder holds, read only when its Information asks for it: a listing never walks a folder. */
interface FolderSizeRepository {
    /** The bytes of every file under the folder at [path], read off the main thread; throws FileOperationException. */
    suspend fun folderSizeOf(path: String): Long
}
