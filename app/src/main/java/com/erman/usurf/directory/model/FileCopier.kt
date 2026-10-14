package com.erman.usurf.directory.model

import java.io.File

/** Copies files and folders into a folder, through the file API or, where that is refused, the granted SD card tree. */
interface FileCopier {
    /**
     * Copies [source], a file or a whole folder, into [destinationFolder] under its own name and returns the copy.
     * Throws [FileOperationException] when [destinationFolder] is [source] or lies inside it, when an item of that
     * name is already there, or when the copy fails, after removing whatever the failed copy created.
     */
    fun copy(source: File, destinationFolder: File): CopiedItem
}

/** An item a copy created, read back from where it was written. */
interface CopiedItem {
    /** The names and sizes the copy holds, or null when it cannot be read. */
    fun shape(): EntryShape?

    /** Deletes the copy and everything inside it; true when nothing of it is left. */
    fun remove(): Boolean
}
