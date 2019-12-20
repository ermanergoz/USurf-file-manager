package com.erman.drawerfm.utilities

import android.os.Build
import android.os.StatFs

fun getTotalStorage(path: String): Long {
    val stat = StatFs(path)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        return stat.totalBytes
    }
    return 0
}

fun getUsedStorage(path: String): Long {
    val stat = StatFs(path)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        val free = stat.availableBlocksLong
        val blockSize = stat.blockSizeLong
        val total = stat.totalBytes
        return total - (free * blockSize)
    }
    return 0
}

fun getUsedStoragePercentage(path: String): Int {
    if (path != "/")
        return ((getUsedStorage(path) * 100 / getTotalStorage(path))).toInt()
    return 0
}