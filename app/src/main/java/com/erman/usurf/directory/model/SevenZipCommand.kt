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

    // The output folder is used exactly as given. Without it 7-Zip replaces every * in that path with the archive name.
    private const val OUTPUT_DIRECTORY_AS_GIVEN = "-spod"

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

    fun extract(archivePath: String, outputDirectory: String): Array<String> = arrayOf(
        PROGRAM_NAME,
        EXTRACT,
        NO_SYMBOLIC_LINKS,
        NO_WILDCARDS,
        OUTPUT_DIRECTORY_AS_GIVEN,
        OUTPUT_DIRECTORY_SWITCH + outputDirectory,
        END_OF_SWITCHES,
        archivePath
    )
}

/** 7-Zip's exit codes, as defined in CPP/7zip/UI/Common/ExitCode.h. */
enum class SevenZipExitCode(val code: Int) {
    SUCCESS(0),
    WARNING(1),
    FATAL_ERROR(2),
    COMMAND_LINE_ERROR(7),
    OUT_OF_MEMORY(8),
    USER_STOPPED(255);

    companion object {
        fun fromCode(code: Int): SevenZipExitCode? = entries.firstOrNull { it.code == code }

        /** The code for a log line: "2 (FATAL_ERROR)", or "3 (unknown)" for a code 7-Zip does not define. */
        fun describe(code: Int): String = "$code (${fromCode(code)?.name ?: "unknown"})"
    }
}
