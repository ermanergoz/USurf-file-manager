package com.erman.usurf.directory.utils

import android.webkit.MimeTypeMap

object MimeUtils {
    fun getMimeTypeForPath(path: String): String {
        val fileName: String = path.substringAfterLast(PATH_SEPARATOR_CHAR, path)
        val extension: String =
            fileName.substringAfterLast(FILE_EXTENSION_SEPARATOR, missingDelimiterValue = "").lowercase()
        if (extension.isEmpty()) return MIME_TYPE_ALL
        val fromMap: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return fromMap ?: MIME_TYPE_ALL
    }
}
