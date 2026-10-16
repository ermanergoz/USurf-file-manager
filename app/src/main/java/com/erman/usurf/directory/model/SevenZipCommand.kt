package com.erman.usurf.directory.model

/**
 * Builds the argument vectors handed to 7-Zip. Every path is one element of the array, passed to 7-Zip as it is,
 * so no character in a file name needs quoting or escaping.
 */
object SevenZipCommand {
    private const val PROGRAM_NAME = "7z"
    private const val ADD = "a"
    private const val EXTRACT = "x"
    private const val ARCHIVE_TYPE_SWITCH = "-t"
    private const val MAXIMUM_COMPRESSION = "-mx=9"
    private const val NO_SYMBOLIC_LINKS = "-snl-"
    private const val OUTPUT_DIRECTORY_SWITCH = "-o"

    // A file name is matched literally, never as a wildcard pattern (a name may contain * or ?).
    private const val NO_WILDCARDS = "-spd"

    // Everything after this marker is a path, even when it starts with - or @.
    private const val END_OF_SWITCHES = "--"

    // 7-Zip replaces every * in the output folder with the archive name and has no way to escape it.
    private const val ARCHIVE_NAME_PLACEHOLDER = '*'

    fun compress(archivePath: String, inputPaths: List<String>, archiveType: String): Array<String> {
        // With no input 7-Zip would add everything in its working directory.
        require(inputPaths.isNotEmpty()) { "Nothing to compress" }
        return arrayOf(
            PROGRAM_NAME,
            ADD,
            ARCHIVE_TYPE_SWITCH + archiveType,
            MAXIMUM_COMPRESSION,
            NO_WILDCARDS,
            END_OF_SWITCHES,
            archivePath
        ) + inputPaths
    }

    fun extract(archivePath: String, outputDirectory: String): Array<String> {
        if (ARCHIVE_NAME_PLACEHOLDER in outputDirectory) {
            throw FileOperationException("The files would be extracted somewhere else: the folder path contains *")
        }
        return arrayOf(
            PROGRAM_NAME,
            EXTRACT,
            NO_SYMBOLIC_LINKS,
            NO_WILDCARDS,
            OUTPUT_DIRECTORY_SWITCH + outputDirectory,
            END_OF_SWITCHES,
            archivePath
        )
    }
}
