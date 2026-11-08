package com.erman.usurf.storage.domain

private const val PATH_SEPARATOR: String = "/"

// Android 11, API 30, from which on another app's Android/data and Android/obb folders refuse the file API.
private const val RESTRICTED_APP_DATA_SDK: Int = 30
private const val ANDROID_FOLDER: String = "Android"
private val RESTRICTED_APP_DATA_FOLDERS: Set<String> = setOf("data", "obb")
private const val APP_SEGMENT_INDEX: Int = 2

interface StoragePathsProvider {
    fun getStorageDirectories(): Set<String>

    /**
     * The storage directory holding [path] (the deepest one when directories nest), or null when
     * [path] is under none of them. The file system root never counts as a storage directory here.
     */
    fun volumeFor(path: String): String? = volumeAmong(getStorageDirectories(), path)
}

/**
 * Whether writing into [folder] needs the root shell, decided from the path alone. Outside every storage directory (the
 * system partitions, another app's data, the file system root) it does when [isWritable] says the app cannot write
 * [folder] itself, as it can its own cache; [isWritable] is read only there. On a storage directory it never does,
 * since a write the file API refuses on a card goes through the card's granted document tree, with one exception: from
 * API 30 on ([sdkInt]) another app's folder under `Android/data` or `Android/obb` refuses the file API even with
 * all-files access and no document tree reaches it, so only root can write there; the folders of [packageName] never
 * need it.
 */
fun StoragePathsProvider.needsRootToWrite(folder: String, isWritable: (String) -> Boolean, sdkInt: Int, packageName: String): Boolean {
    val volume = volumeFor(folder) ?: return !isWritable(folder)
    return sdkInt >= RESTRICTED_APP_DATA_SDK && isOtherAppsData(folder.removePrefix(volume), packageName)
}

// The folder of one app under Android/data or Android/obb: [volumeRelative] starts at the volume, with its separator.
private fun isOtherAppsData(volumeRelative: String, packageName: String): Boolean {
    val segments = volumeRelative.split(PATH_SEPARATOR).filter { it.isNotEmpty() }
    return segments.size > APP_SEGMENT_INDEX &&
        segments[0] == ANDROID_FOLDER &&
        segments[1] in RESTRICTED_APP_DATA_FOLDERS &&
        segments[APP_SEGMENT_INDEX] != packageName
}

/** The directory of [storageDirectories] holding [path], by the rules of [StoragePathsProvider.volumeFor]. */
fun volumeAmong(storageDirectories: Set<String>, path: String): String? = storageDirectories
    .filter { it != PATH_SEPARATOR && it.isNotEmpty() }
    .filter { path == it || path.startsWith(it + PATH_SEPARATOR) }
    .maxByOrNull { it.length }
