package com.erman.usurf.directory.ui

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.erman.usurf.R
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_HEIGHT
import com.erman.usurf.directory.utils.THUMBNAIL_IMAGE_WIDTH

@GlideModule
class ThumbnailGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
            .format(DecodeFormat.PREFER_RGB_565) //uses less memory than default ARGB_8888
            .override(THUMBNAIL_IMAGE_WIDTH, THUMBNAIL_IMAGE_HEIGHT)
            .placeholder(R.drawable.ic_file)

        builder.setDefaultRequestOptions(requestOptions)
            .setLogLevel(Log.ERROR) //Info and Warn are too annoying
    }
}
