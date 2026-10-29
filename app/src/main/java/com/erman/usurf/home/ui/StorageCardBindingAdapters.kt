package com.erman.usurf.home.ui

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.erman.usurf.R
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.storage.domain.StorageKind
import com.erman.usurf.utils.SizeFormatter
import com.erman.usurf.utils.displayNameRes

/** The card's name, in the language the screen shows now. */
@BindingAdapter("storageName")
fun setStorageName(view: TextView, kind: StorageKind?) {
    view.text = kind?.let { view.resources.getString(it.displayNameRes) }.orEmpty()
}

/** The card's used and total space, as "used / total" in the language the screen shows now. */
@BindingAdapter("storageUsage")
fun setStorageUsage(view: TextView, item: StorageItem?) {
    val resources = view.resources
    view.text = item?.let {
        resources.getString(
            R.string.storage_usage_format,
            SizeFormatter.storageSize(resources, it.usedBytes),
            SizeFormatter.storageSize(resources, it.totalBytes)
        )
    }.orEmpty()
}
