package com.erman.usurf.utils

import android.util.Log
import com.erman.usurf.application.MainApplication.Companion.appContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

inline fun <reified T> T.logd(message: String) {
    val sender = T::class.java.simpleName.take(23)
    Log.d(sender, message)
    logToFile("Debug", "$sender : $message")
}

inline fun <reified T> T.loge(message: String) {
    val sender = T::class.java.simpleName.take(23)
    Log.e(sender, message)
    logToFile("Error", "$sender : $message")
}

inline fun <reified T> T.logi(message: String) {
    val sender = T::class.java.simpleName.take(23)
    Log.i(sender, message)
    logToFile("Info", message)
}

fun logToFile(type: String, message: String) {
    try {
        val direct = File(appContext.getExternalFilesDir(null)?.absolutePath + "/logs")

        if (!direct.exists() && !direct.mkdir()) {
            Log.e("Log", "Unable to create log directory")
        }

        val fileTimeStamp = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val timeStamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val fileName = "$type-$fileTimeStamp.txt"

        val file = File(appContext.getExternalFilesDir(null)?.absolutePath + File.separator + "logs" + File.separator + fileName)
        file.createNewFile()

        if (file.exists()) {
            val fileOutputStream = FileOutputStream(file, true)
            val output = timeStamp + "\n" + message + "\n\n"

            fileOutputStream.write(output.toByteArray())
            fileOutputStream.close()
        }
    } catch (err: Exception) {
        Log.e("Log", "Error while logging into file : $err")
    }
}