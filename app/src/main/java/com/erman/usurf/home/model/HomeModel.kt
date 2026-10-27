package com.erman.usurf.home.model

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.erman.usurf.R
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException
import java.util.Locale

private const val PERCENTAGE_BASE: Int = 100
private const val BYTES_PER_KB: Long = 1024L
private const val BYTES_PER_MB: Long = BYTES_PER_KB * 1024L
private const val BYTES_PER_GB: Long = BYTES_PER_MB * 1024L
private const val BYTES_PER_TB: Long = BYTES_PER_GB * 1024L
private const val SIZE_FORMAT: String = "%.1f"

// The units a storage size reads in, from the largest down.
private val STORAGE_UNITS: List<Pair<Long, Int>> =
    listOf(BYTES_PER_TB to R.string.tb, BYTES_PER_GB to R.string.gb, BYTES_PER_MB to R.string.mb, BYTES_PER_KB to R.string.kb)

class HomeModel(private val context: Context) {
    /** One card per directory of [storageDirectories], with its usage read from the volume. */
    fun getStorageItems(storageDirectories: Set<String>): List<StorageItem> {
        val internalPath: String = getInternalStoragePath()
        return storageDirectories.map { path ->
            val isExternal: Boolean = path != ROOT_DIRECTORY && path != internalPath
            StorageItem(
                path = path,
                usedPercentage = getUsedStoragePercentage(path),
                displayName = getDisplayName(path, internalPath),
                usedSize = formatStorageSize(getUsedStorage(path)),
                totalSize = formatStorageSize(getTotalStorage(path)),
                isExternal = isExternal
            )
        }
    }

    private fun getInternalStoragePath(): String = try {
        File(Environment.getExternalStorageDirectory().absolutePath).canonicalPath
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        Environment.getExternalStorageDirectory().absolutePath
    }

    private fun getDisplayName(path: String, internalPath: String): String = when (path) {
        ROOT_DIRECTORY -> context.getString(R.string.root_storage)
        internalPath -> context.getString(R.string.internal_storage)
        else -> context.getString(R.string.sd_card)
    }

    /** [bytes] in the largest unit of [STORAGE_UNITS] it reaches, with one decimal; below a kilobyte, in kilobytes. */
    internal fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0L) return "0 ${context.getString(R.string.bytes)}"
        val (unitBytes, unitName) = STORAGE_UNITS.firstOrNull { (unitBytes, _) -> bytes >= unitBytes } ?: STORAGE_UNITS.last()
        return String.format(Locale.getDefault(), "$SIZE_FORMAT %s", bytes.toDouble() / unitBytes, context.getString(unitName))
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

    fun getUsedStoragePercentage(path: String): Int {
        if (path != ROOT_DIRECTORY && getTotalStorage(path) != 0L) {
            return (getUsedStorage(path) * PERCENTAGE_BASE / getTotalStorage(path)).toInt()
        }
        return 0
    }
}
