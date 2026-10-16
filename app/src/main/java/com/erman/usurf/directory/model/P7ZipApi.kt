package com.erman.usurf.directory.model

/** Runs one 7-Zip command and returns its exit code. Blocks the calling thread until 7-Zip finishes. */
fun interface SevenZipExecutor {
    fun execute(arguments: Array<String>): Int
}

internal object P7ZipApi {
    init {
        System.loadLibrary("p7zip")
    }

    external fun executeCommand(arguments: Array<String>): Int
}
