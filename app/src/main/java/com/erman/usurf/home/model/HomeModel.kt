package com.erman.usurf.home.model

import android.os.Environment
import android.os.StatFs
import com.erman.usurf.storage.domain.storageKindOf
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException

class HomeModel {
    /** One card per directory of [storageDirectories], with its usage read from the volume. */
    fun getStorageItems(storageDirectories: Set<String>): List<StorageItem> {
        val internalPath: String = getInternalStoragePath()
        return storageDirectories.map { path ->
            StorageItem(
                path = path,
                kind = storageKindOf(path, internalPath),
                usedBytes = getUsedStorage(path),
                totalBytes = getTotalStorage(path)
            )
        }
    }

    private fun getInternalStoragePath(): String = try {
        File(Environment.getExternalStorageDirectory().absolutePath).canonicalPath
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        Environment.getExternalStorageDirectory().absolutePath
    }

    // StatFs refuses a path the volume cannot be read at with an IllegalArgumentException; the card then reads as empty.
    private fun getTotalStorage(path: String): Long = try {
        StatFs(path).totalBytes
    } catch (err: IllegalArgumentException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        0L
    }

    private fun getUsedStorage(path: String): Long = try {
        val stat = StatFs(path)
        val free: Long = stat.availableBlocksLong
        val blockSize: Long = stat.blockSizeLong
        val total: Long = stat.totalBytes
        total - (free * blockSize)
    } catch (err: IllegalArgumentException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        0L
    }
}
