package com.erman.usurf.home.model

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.R
import com.erman.usurf.databinding.StorageButtonBinding
import kotlinx.android.synthetic.main.storage_button.view.*
import java.io.File
import java.io.IOException

class HomeModel() {
    fun getStorageDirectories(): ArrayList<String> {
        val paths = arrayListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (file in appContext.getExternalFilesDirs("external")) {
                if (file != null) {
                    val index = file.absolutePath.lastIndexOf("/Android/data")
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath

                    } catch (err: IOException) {
                        err.printStackTrace()
                    }
                    paths.add(path)
                }
            }
        }

        if (paths.isEmpty() || paths.size == 1) {
            //Datas
            if (File("/data/sdext4").exists() && File("/data/sdext4").canRead()) {
                paths.add("/data/sdext4")
            }
            if (File("/data/sdext3").exists() && File("/data/sdext3").canRead()) {
                paths.add("/data/sdext3")
            }
            if (File("/data/sdext2").exists() && File("/data/sdext2").canRead()) {
                paths.add("/data/sdext2")
            }
            if (File("/data/sdext1").exists() && File("/data/sdext1").canRead()) {
                paths.add("/data/sdext1")
            }
            if (File("/data/sdext").exists() && File("/data/sdext").canRead()) {
                paths.add("/data/sdext")
            }

            //Storages
            if (File("/storage/removable/sdcard1").exists() && File("/storage/removable/sdcard1").canRead()) {
                paths.add("/storage/removable/sdcard1")
            }
            if (File("/storage/external_SD").exists() && File("/storage/external_SD").canRead()) {
                paths.add("/storage/external_SD")
            }
            if (File("/storage/ext_sd").exists() && File("/storage/ext_sd").canRead()) {
                paths.add("/storage/ext_sd")
            }
            if (File("/storage/extsd").exists() && File("/storage/extsd").canRead()) {
                paths.add("/storage/extsd")
            }
            if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File(
                    "/storage/sdcard1"
                ).exists() && File(
                    "/storage/sdcard1"
                ).canRead()
            ) {
                paths.add("/storage/sdcard1")
            }
            if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File(
                    "/storage/sdcard0"
                ).exists() && File(
                    "/storage/sdcard0"
                ).canRead()
            ) {
                paths.add("/storage/sdcard0")
            }
            if (File("/storage/sdcard").exists() && File("/storage/sdcard").canRead()) {
                paths.add("/storage/sdcard")
            }
            if (paths.size == 0) {
                paths.add(Environment.getExternalStorageDirectory().absolutePath)
            }

            //MNTS
            if (File("/mnt/sdcard/external_sd").exists() && File("/mnt/sdcard/external_sd").canRead()) {
                paths.add("/mnt/sdcard/external_sd")
            }
            if (File("/mnt/extsdcard").exists() && File("/mnt/extsdcard").canRead()) {
                paths.add("/mnt/extsdcard")
            }
            if (File("/mnt/external_sd").exists() && File("/mnt/external_sd").canRead()) {
                paths.add("/mnt/external_sd")
            }
            if (File("/mnt/externalsd").exists() && File("/mnt/externalsd").canRead()) {
                paths.add("/mnt/externalsd")
            }
            if (File("/mnt/emmc").exists() && File("/mnt/emmc").canRead()) {
                paths.add("/mnt/emmc")
            }
            if (File("/mnt/sdcard0").exists() && File("/mnt/sdcard0").canRead()) {
                paths.add("/mnt/sdcard0")
            }
            if (File("/mnt/sdcard1").exists() && File("/mnt/sdcard1").canRead()) {
                paths.add("/mnt/sdcard1")
            }
            if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File(
                    "/mnt/sdcard"
                ).exists() && File(
                    "/mnt/sdcard"
                ).canRead()
            ) {
                paths.add("/mnt/sdcard")
            }
        }

        if (File("/").exists() && File("/").canRead() && appContext.getSharedPreferences(
                "com.erman.draverfm",
                Context.MODE_PRIVATE
            ).getBoolean(
                "root access",
                false
            )
        ) {
            paths.add("/")
        }
        return paths
    }

    fun createStorageButtons(): MutableList<StorageButtonBinding> {
        //TODO: Move this function to view model
        val storageButtons: MutableList<StorageButtonBinding> = mutableListOf<StorageButtonBinding>()
        val storageDirectories = getStorageDirectories()

        for (i in storageDirectories.indices) {
            val binding: StorageButtonBinding = DataBindingUtil.inflate(LayoutInflater.from(appContext), R.layout.storage_button, null, false)
            storageButtons.add(binding)
            storageButtons[i].root.tag = storageDirectories.elementAt(i)
            storageButtons[i].root.buttonText.text=storageDirectories.elementAt(i)
            storageButtons[i].root.progressBar?.scaleY=20f
            //It is null on older versions of android because I removed it from the layout
        }
        return storageButtons
    }

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
        if (path != "/" && (getTotalStorage(path)).toInt() != 0) return ((getUsedStorage(
            path
        ) * 100 / getTotalStorage(path))).toInt()
        return 0
    }

    //fun getFolderUsedStoragePercentage(path: String): Double {
    //    if (path != "/" && (getTotalStorage(path)).toInt() != 0) return ((getFolderSize(
    //        path
    //    ) * 100.0 / getTotalStorage(path)))
    //    return 0.0
    //}

    //fun getFolderUsedStoragePercentage(path: String, size: Long): Double {
    //    if (path != "/" && (getTotalStorage(path)).toDouble() != 0.0) return ((size * 100.0 / getTotalStorage(
    //        path
    //    )))
    //    return 0.0
    //}


}