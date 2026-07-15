package com.erman.usurf.utils

import android.util.Log
import com.erman.usurf.BuildConfig

const val LOG_TAG_MAX_LENGTH: Int = 23

inline fun <reified T> T.logd(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(LOG_TAG_MAX_LENGTH)
        Log.d(sender, message)
    }
}

inline fun <reified T> T.loge(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(LOG_TAG_MAX_LENGTH)
        Log.e(sender, message)
    }
}

inline fun <reified T> T.logi(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(LOG_TAG_MAX_LENGTH)
        Log.i(sender, message)
    }
}
