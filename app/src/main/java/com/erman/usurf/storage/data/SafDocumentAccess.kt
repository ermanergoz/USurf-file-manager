package com.erman.usurf.storage.data

import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Reaches files on the SD card through the document tree the user granted, for writes the file API is refused. */
interface SafDocumentAccess {
    /**
     * The document for [file] inside the granted tree, creating the folders on the way and, when
     * missing, the item itself (a folder when [isDirectory], else a file). Null when no tree is
     * granted, when [file] lies outside the granted tree, or when the tree cannot be read; never
     * a stand-in document such as the tree root.
     */
    fun documentFor(file: File, isDirectory: Boolean): DocumentFile?

    /**
     * The document for [file] inside the granted tree when it and every folder above it already exist.
     * Never creates anything; null in the same cases as [documentFor] and whenever any part of the path is missing.
     */
    fun existingDocumentFor(file: File): DocumentFile?
}
