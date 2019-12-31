package com.erman.drawerfm.utilities

import android.util.Log
import java.io.File
import java.io.FileFilter

class FileSearchFilter(var searchQuery: String) : FileFilter {
    override fun accept(file: File?): Boolean {
        Log.e("result", file!!.nameWithoutExtension.matches(("movie+").toRegex()).toString())
        return file!!.nameWithoutExtension.matches(("movie+").toRegex())
        //TODO: This is wrong. Fix that
    }
}