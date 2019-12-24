package com.erman.drawerfm.utilities

import android.os.Build
import android.os.StatFs
import kotlin.math.round

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

fun getConvertedFileSize(size: Long): String {
    var sizeStr = ""

    val kilobyte = size / 1024.0
    val megabyte = size / (1024.0 * 1024.0)
    val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
    val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

    sizeStr = when {
        terabyte > 1 ->
            "%.2f".format(terabyte) + " TB"
        gigabyte > 1 ->
            "%.2f".format(gigabyte) + " GB"
        megabyte > 1 ->
            "%.2f".format(megabyte) + " MB"
        kilobyte > 1 ->
            "%.2f".format(kilobyte) + " KB"
        else ->
            "%.2f".format(size) + " Bytes"
    }
    return sizeStr
}