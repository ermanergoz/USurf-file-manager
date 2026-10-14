package com.erman.usurf.storage.data

import android.content.Context
import android.os.Build
import android.os.Environment
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File

/**
 * [StorageAccessPolicy] from the volumes the app sees, the tree it stored and the platform version. Android 11 and
 * newer write to removable volumes through all-files access, so there no tree is ever asked for.
 */
class StorageAccessPolicyImpl(
    private val storagePathsProvider: StoragePathsProvider,
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val sdkInt: Int,
    private val isRemovableVolume: (String) -> Boolean,
    private val isGrantHeld: (String) -> Boolean
) : StorageAccessPolicy {
    override fun canGrantSafAccess(path: String): Boolean {
        if (sdkInt > Build.VERSION_CODES.Q) return false
        val volume = storagePathsProvider.volumeFor(path) ?: return false
        return isRemovableVolume(volume)
    }

    override fun needsSafGrant(path: String): Boolean = canGrantSafAccess(path) && !holdsStoredGrant()

    private fun holdsStoredGrant(): Boolean = storageDirectoryRepository.getChosenUri()
        ?.takeIf { it.isNotBlank() }
        ?.let(isGrantHeld) == true

    companion object {
        /** Whether the volume mounted at [volumePath] is removable; a path that is no storage volume is not. */
        fun isRemovableStorageVolume(volumePath: String): Boolean = try {
            Environment.isExternalStorageRemovable(File(volumePath))
        } catch (err: IllegalArgumentException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            false
        }

        /** Whether the app still holds the persisted write grant on the tree [treeUri] names. */
        fun holdsWriteGrantOn(context: Context, treeUri: String): Boolean = context.contentResolver.persistedUriPermissions.any {
            it.isWritePermission && it.uri.toString() == treeUri
        }
    }
}
