package com.erman.usurf.directory.model

import java.io.File

/** The name and byte size of an item and the shapes of everything inside it; two copies of the same data have equal shapes. */
data class EntryShape(val name: String, val size: Long, val children: Set<EntryShape>?) {
    companion object {
        /** The shape of [file], or null when it does not exist or a folder inside it cannot be listed. */
        fun of(file: File): EntryShape? = if (file.exists()) {
            shapeOf(file, File::getName, File::isDirectory, File::length) { it.listFiles()?.toList() }
        } else {
            null
        }
    }
}

/** The shape of any tree of items, read through [name], [isDirectory], [size] and [children]; null when any part cannot be read. */
fun <T> shapeOf(item: T, name: (T) -> String?, isDirectory: (T) -> Boolean, size: (T) -> Long, children: (T) -> List<T>?): EntryShape? {
    val itemName = name(item) ?: return null
    if (!isDirectory(item)) return EntryShape(itemName, size(item), null)
    val childShapes = children(item)?.map { child -> shapeOf(child, name, isDirectory, size, children) ?: return null } ?: return null
    return EntryShape(itemName, 0, childShapes.toSet())
}
