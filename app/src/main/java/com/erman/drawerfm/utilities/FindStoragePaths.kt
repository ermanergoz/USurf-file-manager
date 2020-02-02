package com.erman.drawerfm.utilities

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

fun getStorageDirectories(context: Context): Set<String> {
    val paths = mutableSetOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                var path = file.absolutePath.substring(0, index)
                try {
                    path = File(path).canonicalPath

                } catch (e: IOException) {
                    Log.e("IOException", "getStorageDirectories")
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
        if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File("/storage/sdcard1").exists() && File(
                "/storage/sdcard1").canRead()) {
            paths.add("/storage/sdcard1")
        }
        if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File("/storage/sdcard0").exists() && File(
                "/storage/sdcard0").canRead()) {
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
        if (!(paths.contains("/storage/emulated/0") || paths.contains("/storage/sdcard")) && File("/mnt/sdcard").exists() && File(
                "/mnt/sdcard").canRead()) {
            paths.add("/mnt/sdcard")
        }
    }

    if (File("/").exists() && File("/").canRead() && context.getSharedPreferences("com.erman.draverfm",
                                                                                  Context.MODE_PRIVATE).getBoolean("root access",
                                                                                                                   false)) {
        paths.add("/")
    }
    return paths
}