package com.erman.usurf.storage.domain

private const val VOLUME_SEPARATOR: Char = ':'
private const val PATH_SEPARATOR: Char = '/'

/**
 * A document tree the user granted, named the way the external storage provider names it:
 * the id of the volume it lives on (the card's file system UUID, which is also the name of
 * its mount folder under `/storage`) and the folder on that volume it starts at.
 */
data class GrantedTree(val volumeId: String, val folder: String) {
    /** The tree's folder as a path on the volume mounted at [volumePath], or null when that is another volume. */
    fun folderOn(volumePath: String): String? = when {
        !volumePath.substringAfterLast(PATH_SEPARATOR).equals(volumeId, ignoreCase = true) -> null
        folder.isEmpty() -> volumePath
        else -> "$volumePath$PATH_SEPARATOR$folder"
    }

    companion object {
        /** Reads a tree document id such as `0000-0000:Music`; null when it names no volume. */
        fun fromDocumentId(documentId: String): GrantedTree? {
            val volumeId = documentId.substringBefore(VOLUME_SEPARATOR, missingDelimiterValue = "")
            val folder = documentId.substringAfter(VOLUME_SEPARATOR).trim(PATH_SEPARATOR)
            return if (volumeId.isEmpty()) null else GrantedTree(volumeId, folder)
        }
    }
}
