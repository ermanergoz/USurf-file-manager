package com.erman.usurf.directory.model

data class  FileModel(
    var path: String = "",
    var name: String = "",
    var nameWithoutExtension: String = "",
    var size: String = "",
    var isDirectory: Boolean = false,
    var lastModified: String = "",
    var extension: String = "",
    var subFileCount: String = "",
    var permission: String = "",
    var isHidden: Boolean = false,
    var isInRoot: Boolean = false,
    var isSelected: Boolean = false
)
