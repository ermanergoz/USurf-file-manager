package com.erman.usurf.storage.domain

import androidx.lifecycle.LiveData

/** The storage directories for the screens that list them, as they change. */
interface StorageDirectoriesSource {
    /**
     * What [StoragePathsProvider.getStorageDirectories] returns, read off the main thread when first observed and
     * emitted again only when it changes: root access turned on or off, or a volume [refreshStorageDirectories] found.
     */
    val storageDirectories: LiveData<Set<String>>

    /**
     * Looks for the volumes again, on the caller's thread, which is never the main thread, and returns the storage
     * directories as they are now; a change also reaches [storageDirectories].
     */
    fun refreshStorageDirectories(): Set<String>
}
