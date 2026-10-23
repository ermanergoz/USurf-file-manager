package com.erman.usurf.directory.model

import java.io.File

/**
 * Builds the shell lines the root shell runs: one line per action, every path absolute and passed as one literal
 * argument, so no line depends on the folder an earlier command left the shell in.
 */
internal object RootCommands {
    /**
     * Wraps an arbitrary file name or path so it is a single literal argument in a POSIX shell,
     * preventing quotes and metacharacters in a name from breaking out of the command (root injection).
     */
    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    /**
     * Lists [folder] with a slash after each folder name and hidden entries included; fails and lists nothing when
     * [folder] cannot be entered. An entry ls cannot read, as in /proc, leaves out that entry only: the listing does not
     * fail on the exit status of ls, and its error messages never become rows.
     */
    fun list(folder: String): String = "cd ${quote(folder)} && { ls -p -a 2>/dev/null; :; }"

    /** Creates the folder [name] in [parent]; fails when anything of that name is there or [parent] no longer exists. */
    fun createFolder(parent: String, name: String): String = "mkdir ${quote(childOf(parent, name))}"

    /** Creates the empty file [name] in [parent] only when nothing of that name is there, so no file is emptied. */
    fun createFile(parent: String, name: String): String {
        val target = childOf(parent, name)
        return "${unlessTaken(target)} : > ${quote(target)}"
    }

    /** Deletes [path] and, for a folder, everything inside it. */
    fun delete(path: String): String = "rm -r ${quote(path)}"

    /** Renames [sourcePath] to [targetPath] only when nothing exists there yet, so no file, folder or link is replaced. */
    fun rename(sourcePath: String, targetPath: String): String = "${unlessTaken(targetPath)} mv ${quote(sourcePath)} ${quote(targetPath)}"

    /** Copies [sourcePath], a file or a whole folder, into [destinationFolder] under its own name, only onto a free name. */
    fun copyInto(sourcePath: String, destinationFolder: String): String {
        val source = File(sourcePath)
        val target = File(destinationFolder, source.name).path
        return "${unlessTaken(target)} cp -a ${quote(source.path)} ${quote(target)}"
    }

    /** Moves [sourcePath] into [destinationFolder] under its own name, only onto a free name. */
    fun moveInto(sourcePath: String, destinationFolder: String): String {
        val source = File(sourcePath)
        return rename(source.path, File(destinationFolder, source.name).path)
    }

    /** Remounts the root file system with [mountOption]'s mode. */
    fun remountRoot(mountOption: MountOption): String = "mount -o ${mountOption.option},remount /"

    // A link is checked apart from what it points to, since -e follows it and is false for a link to nothing.
    private fun unlessTaken(path: String): String = "[ ! -e ${quote(path)} ] && [ ! -L ${quote(path)} ] &&"

    private fun childOf(parent: String, name: String): String = File(parent, name).path
}
