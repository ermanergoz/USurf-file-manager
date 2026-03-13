package com.erman.drawerfm.utilities

import java.io.File
import java.io.FileFilter

class FileSearchFilter(var searchQuery: String) : FileFilter {
    override fun accept(file: File?): Boolean {
        return searchQuery.decapitalize().toRegex().containsMatchIn(file!!.nameWithoutExtension.decapitalize())
    }
}