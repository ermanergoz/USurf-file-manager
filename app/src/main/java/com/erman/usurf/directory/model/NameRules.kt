package com.erman.usurf.directory.model

import java.io.File

// The longest name a Linux file system takes, counted in UTF-8 bytes.
private const val NAME_MAX_BYTES: Int = 255

/** The one rule a name typed for a new or renamed item follows, so the name dialog and the model agree. */
object NameRules {
    /**
     * Why [name] cannot name an item, or null when it can: empty or blank, a dot entry, a path separator or a null
     * character in it, or longer than a file system takes.
     */
    fun validate(name: String): OperationError? {
        val isInvalid =
            name.isBlank() ||
                name == "." ||
                name == ".." ||
                name.contains(File.separatorChar) ||
                name.contains('\u0000') ||
                name.toByteArray(Charsets.UTF_8).size > NAME_MAX_BYTES
        return if (isInvalid) OperationError.InvalidName else null
    }
}
