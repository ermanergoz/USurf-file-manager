package com.erman.usurf.directory.model

data class FileModel(
    val path: String = "",
    val name: String = "",
    val nameWithoutExtension: String = "",
    val size: String = "",
    val isDirectory: Boolean = false,
    val lastModified: String = "",
    val extension: String = "",
    val subFileCount: String = "",
    val permission: MountOption = MountOption.OTHER,
    val isHidden: Boolean = false,
    val isInRoot: Boolean = false,
    val isSelected: Boolean = false,
)
