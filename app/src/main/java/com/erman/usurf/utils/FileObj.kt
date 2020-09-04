package com.erman.usurf.utils

import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import java.text.SimpleDateFormat

data class FileModel(
    var path: String = "",
    var name: String = "",
    var size: String = "",
    var isDirectory: Boolean = false,
    var lastModified: String = "",
    var extension: String = "",
    var subFileCount: String = "",
    var isSelected: Boolean = false
)