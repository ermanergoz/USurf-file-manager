package com.erman.usurf.utils

import android.util.Log
import com.erman.usurf.BuildConfig

inline fun <reified T> T.logd(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(23)
        Log.d(sender, message)
    }
}

inline fun <reified T> T.loge(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(23)
        Log.e(sender, message)
    }
}

inline fun <reified T> T.logi(message: String) {
    if (BuildConfig.DEBUG) {
        val sender = T::class.java.simpleName.take(23)
        Log.i(sender, message)
    }
}