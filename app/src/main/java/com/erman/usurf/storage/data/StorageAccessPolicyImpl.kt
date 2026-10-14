package com.erman.usurf.storage.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.erman.usurf.storage.domain.GrantedTree
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
    private val isGrantHeld: (String) -> Boolean,
    private val grantedTreeOf: (String) -> GrantedTree?
) : StorageAccessPolicy {
    override fun canGrantSafAccess(path: String): Boolean = grantableVolumeOf(path) != null

    override fun needsSafGrant(path: String): Boolean {
        val volume = grantableVolumeOf(path) ?: return false
        return !holdsStoredGrantOn(volume)
    }

    private fun grantableVolumeOf(path: String): String? {
        if (sdkInt > Build.VERSION_CODES.Q) return null
        return storagePathsProvider.volumeFor(path)?.takeIf(isRemovableVolume)
    }

    // The app stores one tree, so after the card was swapped it is a grant on the old card: it counts only
    // when the tree lies on the volume asked about, and only while the platform still holds its grant.
    private fun holdsStoredGrantOn(volume: String): Boolean {
        val treeUri = storageDirectoryRepository.getChosenUri()?.takeIf { it.isNotBlank() } ?: return false
        val isOnVolume = grantedTreeOf(treeUri)?.folderOn(volume) != null
        return isOnVolume && isGrantHeld(treeUri)
    }

    companion object {
        /** Whether the volume mounted at [volumePath] is removable; a path that is no storage volume is not. */
        fun isRemovableStorageVolume(volumePath: String): Boolean = try {
            Environment.isExternalStorageRemovable(File(volumePath))
        } catch (err: IllegalArgumentException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            false
        }

        /** The volume and folder the tree [treeUri] names; null when it names no document tree. */
        fun grantedTreeOf(treeUri: String): GrantedTree? = try {
            GrantedTree.fromDocumentId(DocumentsContract.getTreeDocumentId(treeUri.toUri()))
        } catch (err: IllegalArgumentException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            null
        }

        /** Whether the app still holds the persisted write grant on the tree [treeUri] names. */
        fun holdsWriteGrantOn(context: Context, treeUri: String): Boolean = context.contentResolver.persistedUriPermissions.any {
            it.isWritePermission && it.uri.toString() == treeUri
        }
    }
}
