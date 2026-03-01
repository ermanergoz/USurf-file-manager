package com.erman.usurf.home.model

import android.os.StatFs
import com.erman.usurf.home.utils.PERCENTAGE_BASE
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.loge

class HomeModel {
    fun getStorageItems(): List<StorageItem> {
        val storageDirectories = StoragePaths.getStorageDirectories()
        return storageDirectories.map { path ->
            StorageItem(path = path, usedPercentage = getUsedStoragePercentage(path))
        }
    }

    private fun getTotalStorage(path: String): Long {
        return try {
            StatFs(path).totalBytes
        } catch (err: RuntimeException) {
            loge("getTotalStorage Error calculating total storage $err")
            0L
        }
    }

    private fun getUsedStorage(path: String): Long {
        val stat = StatFs(path)
        val free = stat.availableBlocksLong
        val blockSize = stat.blockSizeLong
        val total = stat.totalBytes
        return total - (free * blockSize)
    }

    fun getUsedStoragePercentage(path: String): Int {
        if (path != ROOT_DIRECTORY && getTotalStorage(path) != 0L) {
            return (getUsedStorage(path) * PERCENTAGE_BASE / getTotalStorage(path)).toInt()
        }
        return 0
    }
}
