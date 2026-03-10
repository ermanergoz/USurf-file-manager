package com.erman.usurf.directory.model

internal object P7ZipApi {
    init {
        System.loadLibrary("p7zip")
    }

    external fun executeCommand(command: String): Int
}
