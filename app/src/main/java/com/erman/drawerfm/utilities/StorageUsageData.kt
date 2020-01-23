package com.erman.drawerfm.utilities

import android.os.Build
import android.os.StatFs
import android.util.Log
import java.io.File

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
    if (path != "/" && (getTotalStorage(path)).toInt() != 0) return ((getUsedStorage(path) * 100 / getTotalStorage(path))).toInt()
    return 0
}

fun getFolderUsedStoragePercentage(path: String): Double {
    if (path != "/" && (getTotalStorage(path)).toInt() != 0) return ((getFolderSize(path) * 100.0 / getTotalStorage(path)))
    return 0.0
}

fun getFolderUsedStoragePercentage(path: String, size: Long): Double {
    if (path != "/" && (getTotalStorage(path)).toDouble() != 0.0) return ((size * 100.0 / getTotalStorage(path)))
    return 0.0
}

fun getFolderSize(path: String): Double {
    if (File(path).exists()) {
        var size = 0.0

        var fileList = File(path).listFiles().toList()

        for (i in fileList.indices) {
            if (fileList[i].isDirectory) {
                size += getFolderSize(fileList[i].path)
            } else size += fileList[i].length()
        }
        return size
    }
    return 0.0
}

fun getConvertedFileSize(size: Long): String {
    var sizeStr = ""

    val kilobyte = size / 1024.0
    val megabyte = size / (1024.0 * 1024.0)
    val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
    val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

    sizeStr = when {
        terabyte > 1 -> "%.2f".format(terabyte) + " TB"
        gigabyte > 1 -> "%.2f".format(gigabyte) + " GB"
        megabyte > 1 -> "%.2f".format(megabyte) + " MB"
        kilobyte > 1 -> "%.2f".format(kilobyte) + " KB"
        else -> size.toDouble().toString() + " Bytes"
    }
    return sizeStr
}

fun getConvertedFileSize(size: Double): String {
    var sizeStr = ""

    val kilobyte = size / 1024.0
    val megabyte = size / (1024.0 * 1024.0)
    val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
    val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

    sizeStr = when {
        terabyte > 1 -> "%.2f".format(terabyte) + " TB"
        gigabyte > 1 -> "%.2f".format(gigabyte) + " GB"
        megabyte > 1 -> "%.2f".format(megabyte) + " MB"
        kilobyte > 1 -> "%.2f".format(kilobyte) + " KB"
        else -> size.toDouble().toString() + " Bytes"
    }
    return sizeStr
}