package com.erman.usurf.storage.domain

private const val ROOT_PATH: String = "/"

/** What a storage directory is, which is what the screens name it by. */
enum class StorageKind { ROOT, INTERNAL, SD_CARD }

/** The kind of the storage directory [path], given the path of the device's internal storage, [internalPath]. */
fun storageKindOf(path: String, internalPath: String): StorageKind = when (path) {
    ROOT_PATH -> StorageKind.ROOT
    internalPath -> StorageKind.INTERNAL
    else -> StorageKind.SD_CARD
}
