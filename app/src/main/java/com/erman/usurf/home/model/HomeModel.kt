package com.erman.usurf.home.model

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.erman.usurf.R
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.util.Locale

private const val PERCENTAGE_BASE: Int = 100
private const val BYTES_PER_KB: Long = 1024L
private const val BYTES_PER_MB: Long = BYTES_PER_KB * 1024L
private const val BYTES_PER_GB: Long = BYTES_PER_MB * 1024L
private const val BYTES_PER_TB: Long = BYTES_PER_GB * 1024L
private const val SIZE_FORMAT: String = "%.1f"

class HomeModel(
    private val context: Context,
    private val storagePathsProvider: StoragePathsProvider,
) {
    fun getStorageItems(): List<StorageItem> {
        val storageDirectories = storagePathsProvider.getStorageDirectories()
        val internalPath: String = getInternalStoragePath()
        return storageDirectories.map { path ->
            val isExternal: Boolean = path != ROOT_DIRECTORY && path != internalPath
            StorageItem(
                path = path,
                usedPercentage = getUsedStoragePercentage(path),
                displayName = getDisplayName(path, internalPath),
                usedSize = formatStorageSize(getUsedStorage(path)),
                totalSize = formatStorageSize(getTotalStorage(path)),
                isExternal = isExternal,
            )
        }
    }

    private fun getInternalStoragePath(): String {
        return try {
            File(Environment.getExternalStorageDirectory().absolutePath).canonicalPath
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            Environment.getExternalStorageDirectory().absolutePath
        }
    }

    private fun getDisplayName(path: String, internalPath: String): String {
        return when (path) {
            ROOT_DIRECTORY -> context.getString(R.string.root_storage)
            internalPath -> context.getString(R.string.internal_storage)
            else -> context.getString(R.string.sd_card)
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        return when {
            bytes <= 0L -> "0 ${context.getString(R.string.bytes)}"
            bytes < BYTES_PER_MB -> String.format(
                Locale.getDefault(),
                "$SIZE_FORMAT %s",
                bytes.toDouble() / BYTES_PER_KB,
                context.getString(R.string.kb),
            )
            bytes < BYTES_PER_GB -> String.format(
                Locale.getDefault(),
                "$SIZE_FORMAT %s",
                bytes.toDouble() / BYTES_PER_MB,
                context.getString(R.string.mb),
            )
            bytes < BYTES_PER_TB -> String.format(
                Locale.getDefault(),
                "$SIZE_FORMAT %s",
                bytes.toDouble() / BYTES_PER_GB,
                context.getString(R.string.gb),
            )
            else -> String.format(
                Locale.getDefault(),
                "$SIZE_FORMAT %s",
                bytes.toDouble() / BYTES_PER_TB,
                context.getString(R.string.tb),
            )
        }
    }

    private fun getTotalStorage(path: String): Long {
        return try {
            StatFs(path).totalBytes
        } catch (err: RuntimeException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            0L
        }
    }

    private fun getUsedStorage(path: String): Long {
        return try {
            val stat = StatFs(path)
            val free: Long = stat.availableBlocksLong
            val blockSize: Long = stat.blockSizeLong
            val total: Long = stat.totalBytes
            total - (free * blockSize)
        } catch (err: RuntimeException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            0L
        }
    }

    fun getUsedStoragePercentage(path: String): Int {
        if (path != ROOT_DIRECTORY && getTotalStorage(path) != 0L) {
            return (getUsedStorage(path) * PERCENTAGE_BASE / getTotalStorage(path)).toInt()
        }
        return 0
    }
}
