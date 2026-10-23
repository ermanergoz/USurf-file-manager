package com.erman.usurf.directory.model

import java.io.File
import java.util.Locale

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
    val isSelected: Boolean = false
) {
    /** The extension as the row's badge shows it, upper cased the same in every language. */
    val extensionBadge: String
        get() = extension.uppercase(Locale.ROOT)

    /** The folder this item is in, worked out from its path alone; the top of the file system is its own. */
    val folderPath: String
        get() = File(path).parent ?: File.separator
}
