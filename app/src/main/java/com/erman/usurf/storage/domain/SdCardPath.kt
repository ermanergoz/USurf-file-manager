package com.erman.usurf.storage.domain

private const val PATH_SEPARATOR: String = "/"

/** Where a canonical path lies relative to the folder a granted SD card tree starts at. */
sealed interface SdCardPath {
    /** The path is the tree's own folder. */
    data object Root : SdCardPath

    /** The path is inside the tree; [segments] lead from the tree's folder down to it. */
    data class Relative(val segments: List<String>) : SdCardPath

    /** The path is not inside the tree, so the tree must never be used for it. */
    data object Outside : SdCardPath
}

/**
 * Classifies [canonicalPath] against [base], the folder of a granted tree. A path counts as inside
 * only when it equals [base] or continues it after a separator, so `/storage/0000-00001` is not
 * inside `/storage/0000-0000` and nothing is inside a base of `/`.
 */
fun relativeToSdCard(canonicalPath: String, base: String): SdCardPath {
    val prefix = base + PATH_SEPARATOR
    return when {
        canonicalPath == base -> SdCardPath.Root
        canonicalPath.startsWith(prefix) && base != PATH_SEPARATOR ->
            SdCardPath.Relative(canonicalPath.removePrefix(prefix).split(PATH_SEPARATOR).filter { it.isNotEmpty() })
        else -> SdCardPath.Outside
    }
}
