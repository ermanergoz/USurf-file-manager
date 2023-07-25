package com.erman.usurf.home.model

import android.os.StatFs
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.erman.usurf.R
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.home.utils.PERCENTAGE_BASE
import com.erman.usurf.home.utils.STORAGE_PROGRESS_BAR_SCALE
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.loge

class HomeModel {
    fun createStorageButtons(): MutableList<StorageButtonBinding> {
        val storageButtons: MutableList<StorageButtonBinding> = mutableListOf()
        val storageDirectories = StoragePaths.getStorageDirectories()

        for (i in storageDirectories.indices) {
            val binding: StorageButtonBinding =
                DataBindingUtil.inflate(LayoutInflater.from(appContext), R.layout.storage_button, null, false)
            storageButtons.add(binding)
            storageButtons[i].root.tag = storageDirectories.elementAt(i)
            storageButtons[i].buttonText.text = storageDirectories.elementAt(i)
            storageButtons[i].progressBar?.scaleY = STORAGE_PROGRESS_BAR_SCALE
            //It is null on older versions of android because I removed it from the layout
        }
        return storageButtons
    }

    private fun getTotalStorage(path: String): Long {
        try {
            return StatFs(path).totalBytes
        } catch (err: RuntimeException) {
            loge("getTotalStorage Error calculating total storage $err")
        }
        return 0
    }


    private fun getUsedStorage(path: String): Long {
        val stat = StatFs(path)
        val free = stat.availableBlocksLong
        val blockSize = stat.blockSizeLong
        val total = stat.totalBytes
        return total - (free * blockSize)
    }

    fun getUsedStoragePercentage(path: String): Int {
        if (path != ROOT_DIRECTORY && (getTotalStorage(path)).toInt() != 0) return ((getUsedStorage(
            path
        ) * PERCENTAGE_BASE / getTotalStorage(path))).toInt()
        return 0
    }
}
