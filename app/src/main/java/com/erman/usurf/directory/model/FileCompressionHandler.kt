package com.erman.usurf.directory.model

import com.hzy.libp7zip.P7ZipApi

/*
              0      Normal (no errors or warnings detected)
              1      Warning (Non fatal error(s)). For example, some files cannot be read during compressing. So they were not
                     compressed
              2      Fatal error
              7      Bad command line parameters
              8      Not enough memory for operation
              255    User stopped the process with control-C (or similar)
*/

class FileCompressionHandler {
    fun compress(archiveDirectory: String, sourceDirectory: String): Boolean {
        val res = P7ZipApi.executeCommand("7z a -mx=9 $archiveDirectory $sourceDirectory")

        if (res == 0 || res == 1)
            return true
        return false
    }

    fun extract(archiveDirectory: String): Boolean {
        val res = P7ZipApi.executeCommand("7z x $archiveDirectory") //TODO: Returns 2. Extract files to a specific location

        if (res == 0 || res == 1)
            return true
        return false
    }
}