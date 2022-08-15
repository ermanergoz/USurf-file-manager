package com.erman.usurf.directory.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.erman.usurf.R
import com.erman.usurf.directory.utils.THUMBNAIL_SIZE_MULTIPLIER
import com.erman.usurf.preference.data.PreferenceProvider
import org.koin.java.KoinJavaComponent.getKoin
import java.io.File

@BindingAdapter("filePath")
fun setImage(imageView: ImageView, filePath: String) {
    val preferenceProvider: PreferenceProvider = getKoin().get()
    val file = File(filePath)

    if (file.isFile && preferenceProvider.getShowThumbnailsPreference()) {
        GlideApp.with(imageView.context).load(filePath).thumbnail(THUMBNAIL_SIZE_MULTIPLIER)
            .into(imageView)
    } else if (file.isFile) imageView.setImageResource(R.drawable.ic_file)
    else imageView.setImageResource(R.drawable.ic_folder)
}
