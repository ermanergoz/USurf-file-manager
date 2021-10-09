package com.erman.usurf.directory.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_HEIGHT
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_WIDTH
import com.erman.usurf.directory.utils.THUMBNAIL_SIZE_MULTIPLIER

@BindingAdapter("filePath", "isDirectory", "placeHolder", requireAll = false)
fun setImage(imageView: ImageView, filePath: String, isDirectory: Boolean, placeHolder: Drawable) {
    if (!isDirectory) {
        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
            .override(THUMBNAIL_IMAGE_WIDTH, THUMBNAIL_IMAGE_HEIGHT)
            .placeholder(placeHolder)

        Glide.with(imageView.context).load(filePath).thumbnail(THUMBNAIL_SIZE_MULTIPLIER).apply(requestOptions).into(imageView)
    }
}