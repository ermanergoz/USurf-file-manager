package com.erman.usurf.storage.domain

/** Decides when the app has to ask for the SD card's document tree before it can change files there. */
interface StorageAccessPolicy {
    /**
     * True when [path] lies on a volume the app can only write to through a granted document tree
     * (a removable volume on Android 10 or older), whether or not that tree is granted yet.
     */
    fun canGrantSafAccess(path: String): Boolean

    /**
     * True when writing to [path] needs a tree the app does not hold: [canGrantSafAccess] and no granted tree is
     * still held. Never decided by `File.canWrite()`, which reports true on sdcardfs while the write itself is refused.
     */
    fun needsSafGrant(path: String): Boolean
}
