package com.erman.usurf.utils

import android.annotation.SuppressLint
import com.erman.usurf.application.MainApplication
import com.erman.usurf.preference.data.PreferenceProvider
import java.io.File
import java.io.IOException
import org.koin.java.KoinJavaComponent.getKoin

object StoragePaths {
    private val preferenceProvider: PreferenceProvider = getKoin().get()

    @SuppressLint("SdCardPath")
    fun getStorageDirectories(): Set<String> {
        val paths = mutableSetOf<String>()
        val storageDirectories = arrayOf(
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
            "/mnt/sdcard"
        )

        for (file in MainApplication.appContext.getExternalFilesDirs(DIR_TYPE_EXTERNAL)) {
            file?.let {
                val index = it.absolutePath.lastIndexOf(EXTERNAL_DIR_SUFFIX)
                var path = it.absolutePath.substring(0, index)
                try {
                    path = File(path).canonicalPath
                } catch (err: IOException) {
                    loge("" + err.localizedMessage)
                }
                paths.add(path)
            }
        }
        if (paths.isEmpty() || paths.size == 1) {
            storageDirectories.forEach {
                if (File(it).exists() && File(it).canRead()) {
                    paths.add(it)
                }
            }
        }

        if (File(ROOT_DIRECTORY).exists() && File(ROOT_DIRECTORY).canRead() && preferenceProvider.getRootAccessPreference()) {
            paths.add(ROOT_DIRECTORY)
        }
        return paths
    }
}

const val DIR_TYPE_EXTERNAL: String = "external"
const val EXTERNAL_DIR_SUFFIX: String = "/Android/data"
