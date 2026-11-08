package com.erman.usurf.utils

import androidx.annotation.StringRes
import com.erman.usurf.R
import com.erman.usurf.storage.domain.StorageKind

/** The name a storage directory of this kind goes by on screen. */
@get:StringRes
val StorageKind.displayNameRes: Int
    get() = when (this) {
        StorageKind.ROOT -> R.string.root_storage
        StorageKind.INTERNAL -> R.string.internal_storage
        StorageKind.SD_CARD -> R.string.sd_card
    }
