package com.erman.usurf.directory.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.erman.usurf.R
import com.erman.usurf.directory.model.FileModel

private const val THUMBNAIL_SIZE_MULTIPLIER = 0.1F

@BindingAdapter("filePath", "showThumbnails", "isDirectory")
fun setImage(imageView: ImageView, filePath: String, showThumbnails: Boolean, isDirectory: Boolean) {
    if (isDirectory) {
        imageView.setImageResource(R.drawable.ic_folder)
    } else if (showThumbnails) {
        val requestBuilder: RequestBuilder<Drawable> =
            GlideApp.with(imageView.context)
                .asDrawable().sizeMultiplier(THUMBNAIL_SIZE_MULTIPLIER)

        GlideApp.with(imageView.context).load(filePath).thumbnail(requestBuilder)
            .into(imageView)
    } else {
        imageView.setImageResource(R.drawable.ic_file)
    }
}

/** The rows of the folder or the search on screen, handed to the list's adapter, which redraws only the rows that changed. */
@BindingAdapter("files")
fun setFiles(recyclerView: RecyclerView, files: List<FileModel>?) {
    (recyclerView.adapter as? DirectoryRecyclerViewAdapter)?.updateData(files.orEmpty())
}
