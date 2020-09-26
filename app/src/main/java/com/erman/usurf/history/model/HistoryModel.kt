package com.erman.usurf.history.model

import com.erman.usurf.MainApplication
import java.io.*

class HistoryModel {
    fun readHistoryFile(): String {
        val file = File(MainApplication.appContext.getExternalFilesDir(null)?.absolutePath + File.separator + "logs" + File.separator + "Info.txt")
        if (file.exists()) {
            val bufferedReader: BufferedReader = file.bufferedReader()
            return bufferedReader.use { it.readText() }
        }
        return ""
    }
}