package com.erman.usurf.directory.model

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