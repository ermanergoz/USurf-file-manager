package com.erman.usurf.directory.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.erman.usurf.R
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_HEIGHT
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_WIDTH
import com.erman.usurf.directory.utils.THUMBNAIL_SIZE_MULTIPLIER
import java.io.File

@BindingAdapter("filePath")
fun setImage(imageView: ImageView, filePath: String) {
    val file = File(filePath)
    if (file.isFile) {
        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
            .format(DecodeFormat.PREFER_RGB_565) //uses less memory than default ARGB_8888
            .override(THUMBNAIL_IMAGE_WIDTH, THUMBNAIL_IMAGE_HEIGHT)
            .placeholder(R.drawable.ic_file)

        Glide.with(imageView.context).load(filePath).thumbnail(THUMBNAIL_SIZE_MULTIPLIER).apply(requestOptions).into(imageView)
    } else imageView.setImageResource(R.drawable.ic_folder)
}