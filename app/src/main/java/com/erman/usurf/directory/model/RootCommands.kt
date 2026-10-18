package com.erman.usurf.directory.model

/** Builds the shell lines the root shell runs, with every path passed as one literal argument. */
internal object RootCommands {
    /**
     * Wraps an arbitrary file name or path so it is a single literal argument in a POSIX shell,
     * preventing quotes and metacharacters in a name from breaking out of the command (root injection).
     */
    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    /** Renames [sourcePath] to [targetPath] only when nothing exists there yet, so no file or folder is replaced. */
    fun rename(sourcePath: String, targetPath: String): String =
        "[ ! -e ${quote(targetPath)} ] && mv ${quote(sourcePath)} ${quote(targetPath)}"
}
