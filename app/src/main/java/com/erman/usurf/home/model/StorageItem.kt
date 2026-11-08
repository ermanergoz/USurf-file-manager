package com.erman.usurf.home.model

import com.erman.usurf.storage.domain.StorageKind

private const val PERCENTAGE_BASE: Int = 100

/** One storage card: a volume with its usage in bytes, read from the volume; the screen puts it into words. */
data class StorageItem(val path: String, val kind: StorageKind, val usedBytes: Long = 0L, val totalBytes: Long = 0L) {
    val isExternal: Boolean
        get() = kind == StorageKind.SD_CARD

    /** How full the volume is, from 0 to 100; the file system root shows none. */
    val usedPercentage: Int
        get() = if (kind != StorageKind.ROOT && totalBytes != 0L) (usedBytes * PERCENTAGE_BASE / totalBytes).toInt() else 0
}
