package com.erman.usurf.directory.ui

import android.content.res.Resources
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.erman.usurf.R
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.SizeFormatter
import com.erman.usurf.utils.modificationDateText

/** A folder's entry count or a file's size, as its row shows it; empty when it was not read, as for an entry listed through root. */
fun Resources.fileSummaryText(file: FileModel): String = if (file.isDirectory) {
    file.subFileCount?.let { count -> getQuantityString(R.plurals.file_count, count, count) }.orEmpty()
} else {
    file.size?.let { bytes -> SizeFormatter.fileSize(this, bytes) }.orEmpty()
}

/** What a row says under the name: [fileSummaryText], then when the item last changed, each only when it was read. */
fun Resources.fileMetaText(file: FileModel): String {
    val summary = fileSummaryText(file)
    val date = file.lastModified?.let { millis -> modificationDateText(millis) }.orEmpty()
    return if (summary.isNotEmpty() && date.isNotEmpty()) getString(R.string.file_meta_format, summary, date) else summary + date
}

@BindingAdapter("fileMeta")
fun setFileMeta(view: TextView, file: FileModel?) {
    view.text = file?.let { view.resources.fileMetaText(it) }.orEmpty()
}
