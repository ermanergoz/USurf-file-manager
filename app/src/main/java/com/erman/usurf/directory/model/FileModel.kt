package com.erman.usurf.directory.model

import android.os.Parcelable
import java.io.File
import java.util.Locale
import kotlinx.parcelize.Parcelize

/**
 * One folder or file as a listing read it. [size] is a file's length in bytes, [lastModified] the time of its last
 * change in milliseconds and [subFileCount] the number of entries a folder holds; each is null when it was not read,
 * as for a folder's size or any entry listed through root. The screens put them into words.
 */
@Parcelize
data class FileModel(
    val path: String = "",
    val name: String = "",
    val nameWithoutExtension: String = "",
    val size: Long? = null,
    val isDirectory: Boolean = false,
    val lastModified: Long? = null,
    val extension: String = "",
    val subFileCount: Int? = null,
    val permission: MountOption = MountOption.OTHER,
    val isHidden: Boolean = false,
    val isInRoot: Boolean = false,
    val isSelected: Boolean = false
) : Parcelable {
    /** The extension as the row's badge shows it, upper cased the same in every language. */
    val extensionBadge: String
        get() = extension.uppercase(Locale.ROOT)

    /** The folder this item is in, worked out from its path alone; the top of the file system is its own. */
    val folderPath: String
        get() = File(path).parent ?: File.separator
}
