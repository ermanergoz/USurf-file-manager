package com.erman.usurf.directory.model

import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.storage.domain.needsRootToWrite
import java.io.File

/**
 * Runs the directory model's writes as root: only while the root explorer is on and su grants the shell, with the root
 * file system mounted read-write for the write and read-only again however the write ends.
 */
class RootWriter(
    private val preferencesRepository: PreferencesRepository,
    private val rootHandler: RootHandler,
    private val storagePathsProvider: StoragePathsProvider,
    private val sdkInt: Int,
    private val packageName: String
) {
    /**
     * Whether a write into [folder] takes the root path, from the path alone, so nothing is written to find out and
     * storage is never searched. The rule reads [File.canWrite] only on paths outside every storage directory.
     */
    fun needsRoot(folder: String): Boolean = storagePathsProvider.needsRootToWrite(folder, { File(it).canWrite() }, sdkInt, packageName)

    /** Runs [write] in the root shell; throws [FileOperationException] when root is not available or the write fails. */
    fun write(write: (RootHandler) -> Boolean) {
        val hasRootAccess = preferencesRepository.getRootAccessPreference() && rootHandler.isRootAccessGiven()
        if (!hasRootAccess) throw FileOperationException("Root access is not available")
        rootHandler.remountRootDirAs(MountOption.READ_WRITE)
        // A shell that dropped the write included.
        val isSuccess =
            try {
                write(rootHandler)
            } finally {
                rootHandler.remountRootDirAs(MountOption.READ)
            }
        if (!isSuccess) throw FileOperationException("Root operation failed")
    }
}
