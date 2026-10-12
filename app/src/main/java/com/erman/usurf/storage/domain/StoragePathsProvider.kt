package com.erman.usurf.storage.domain

private const val PATH_SEPARATOR: String = "/"

interface StoragePathsProvider {
    fun getStorageDirectories(): Set<String>

    /**
     * The storage directory holding [path] (the deepest one when directories nest), or null when
     * [path] is under none of them. The file system root never counts as a storage directory here.
     */
    fun volumeFor(path: String): String? = getStorageDirectories()
        .filter { it != PATH_SEPARATOR && it.isNotEmpty() }
        .filter { path == it || path.startsWith(it + PATH_SEPARATOR) }
        .maxByOrNull { it.length }
}
