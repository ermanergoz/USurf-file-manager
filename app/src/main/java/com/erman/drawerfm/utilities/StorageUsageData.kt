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
    Log.e("storage size", ((getFolderSize(path) * 100 / getTotalStorage(path))).toInt().toString())
    if (path != "/" && (getTotalStorage(path)).toInt() != 0)
        return ((getUsedStorage(path) * 100 / getTotalStorage(path))).toInt()
    return 0
}

fun getFileUsedStoragePercentage(path: String): Int {
    Log.e("file size", ((getUsedStorage(path) * 100 / getTotalStorage(path))).toInt().toString())
    if (path != "/" && (getTotalStorage(path)).toInt() != 0)
        return ((getFolderSize(path) * 100 / getTotalStorage(path))).toInt()
    return 0
}

fun getFileUsedStoragePercentage(path: String, size: Long): Int {
    Log.e("file size", ((getUsedStorage(path) * 100 / getTotalStorage(path))).toInt().toString())
    if (path != "/" && (getTotalStorage(path)).toInt() != 0)
        return ((size * 100 / getTotalStorage(path))).toInt()
    return 0
}

fun getFolderSize(path: String): Long {
    if (File(path).exists()) {
        var size: Long = 0

        var fileList = File(path).listFiles().toList()

        for (i in fileList.indices) {
            size += if (fileList[i].isDirectory)
                getFolderSize(fileList[i].path)
            else
                fileList[i].length()
        }
        return size
    }
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
            size.toDouble().toString() + " Bytes"
    }
    return sizeStr
}