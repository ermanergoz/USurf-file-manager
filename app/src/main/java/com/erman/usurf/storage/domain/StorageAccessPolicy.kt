package com.erman.usurf.storage.domain

/** Decides when the app has to ask for the SD card's document tree before it can change files there. */
interface StorageAccessPolicy {
    /**
     * True when [path] lies on a volume the app can only write to through a granted document tree
     * (a removable volume on Android 10 or older), whether or not that tree is granted yet.
     */
    fun canGrantSafAccess(path: String): Boolean

    /**
     * True when writing to [path] needs a tree the app does not hold: [canGrantSafAccess] and no tree still held covers
     * [path], that is starts at [path] or at a folder holding it, so a grant of one folder of the card does not cover the
     * rest of it. Never decided by `File.canWrite()`, which reports true on sdcardfs while the write itself is refused.
     */
    fun needsSafGrant(path: String): Boolean

    /**
     * True when opening the volume of [path] should ask for its tree: [canGrantSafAccess] and no tree on that volume is
     * held at all. A grant of one folder is the user's choice, so it stops this question; a write outside that folder
     * asks through [needsSafGrant] when it happens.
     */
    fun needsFirstSafGrant(path: String): Boolean
}
