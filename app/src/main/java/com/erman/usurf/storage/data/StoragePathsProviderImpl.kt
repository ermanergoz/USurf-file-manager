package com.erman.usurf.storage.data

import android.annotation.SuppressLint
import android.content.Context
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.SINGLE_STORAGE_COUNT
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException

private const val SUBSTRING_START_INDEX: Int = 0
private const val EXTERNAL_DIR_SUFFIX: String = "/Android/data"
private const val DIR_TYPE_EXTERNAL: String = "external"

class StoragePathsProviderImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) : StoragePathsProvider {
    @SuppressLint("SdCardPath")
    private val fallbackStorageDirectories: Array<String> =
        arrayOf(
            "/data/sdext4",
            "/data/sdext3",
            "/data/sdext2",
            "/data/sdext1",
            "/data/sdext",
            "/storage/removable/sdcard1",
            "/storage/external_SD",
            "/storage/ext_sd",
            "/storage/extsd",
            "/storage/sdcard1",
            "/storage/sdcard0",
            "/storage/sdcard",
            "/mnt/sdcard/external_sd",
            "/mnt/extsdcard",
            "/mnt/external_sd",
            "/mnt/externalsd",
            "/mnt/emmc",
            "/mnt/sdcard0",
            "/mnt/sdcard1",
            "/mnt/sdcard",
        )

    override fun getStorageDirectories(): Set<String> {
        val paths = getExternalStoragePaths()
        addFallbackPathsIfNeeded(paths)
        addRootDirectoryIfEnabled(paths)
        return paths
    }

    private fun getExternalStoragePaths(): MutableSet<String> {
        val paths = mutableSetOf<String>()
        for (file in context.getExternalFilesDirs(DIR_TYPE_EXTERNAL)) {
            file?.let { fileDir ->
                val index = fileDir.absolutePath.lastIndexOf(EXTERNAL_DIR_SUFFIX)
                var path = fileDir.absolutePath.substring(SUBSTRING_START_INDEX, index)
                try {
                    path = File(path).canonicalPath
                } catch (err: IOException) {
                    loge(err.localizedMessage ?: UNKNOWN_ERROR)
                }
                paths.add(path)
            }
        }
        return paths
    }

    private fun addFallbackPathsIfNeeded(paths: MutableSet<String>) {
        if (paths.isEmpty() || paths.size == SINGLE_STORAGE_COUNT) {
            fallbackStorageDirectories
                .filter { File(it).exists() && File(it).canRead() }
                .forEach { paths.add(it) }
        }
    }

    private fun addRootDirectoryIfEnabled(paths: MutableSet<String>) {
        if (preferencesRepository.getRootAccessPreference()) {
            paths.add(ROOT_DIRECTORY)
        }
    }
}
