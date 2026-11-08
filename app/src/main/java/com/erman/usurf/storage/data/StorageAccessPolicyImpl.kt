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
import java.util.concurrent.ConcurrentHashMap

private const val PATH_SEPARATOR: String = "/"

/**
 * [StorageAccessPolicy] from the volumes the app sees, the tree it stored and the platform version. Android 11 and
 * newer write to removable volumes through all-files access, so there no tree is ever asked for.
 *
 * The platform is asked once per volume whether it is removable. Opening a card asks once per tree whether its grant
 * is held, so a tap costs no call to the system after the first; a grant not held is asked again each time, so a new
 * one counts at once. A refused write asks the platform every time, since clearing the storage of the documents
 * provider gives up a grant the app still remembers; what it answers also holds for the next tap.
 */
class StorageAccessPolicyImpl(
    private val storagePathsProvider: StoragePathsProvider,
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val sdkInt: Int,
    private val isRemovableVolume: (String) -> Boolean,
    private val isGrantHeld: (String) -> Boolean,
    private val grantedTreeOf: (String) -> GrantedTree?
) : StorageAccessPolicy {
    private val removableVolumes = ConcurrentHashMap<String, Boolean>()
    private val heldTrees = ConcurrentHashMap<String, Boolean>()

    override fun canGrantSafAccess(path: String): Boolean = grantableVolumeOf(path) != null

    override fun needsSafGrant(path: String): Boolean {
        val volume = grantableVolumeOf(path) ?: return false
        return !holdsStoredGrantCovering(path, volume)
    }

    override fun needsFirstSafGrant(path: String): Boolean {
        val volume = grantableVolumeOf(path) ?: return false
        return !holdsStoredGrantOn(volume)
    }

    private fun grantableVolumeOf(path: String): String? {
        if (sdkInt > Build.VERSION_CODES.Q) return null
        return storagePathsProvider.volumeFor(path)?.takeIf { volume -> removableVolumes.getOrPut(volume) { isRemovableVolume(volume) } }
    }

    // The app stores one tree, so after the card was swapped it is a grant on the old card: it counts only
    // when the tree lies on the volume asked about, and only while the platform still holds its grant.
    private fun holdsStoredGrantOn(volume: String): Boolean = heldTreeFolderOn(volume, ::isHeldAsRemembered) != null

    // The tree reaches only the folder it was granted for and what lies inside it.
    private fun holdsStoredGrantCovering(path: String, volume: String): Boolean {
        val folder = heldTreeFolderOn(volume, ::isHeldNow) ?: return false
        return path == folder || path.startsWith("$folder$PATH_SEPARATOR")
    }

    // The stored tree's folder as a path on [volume], while [isHeld] says its grant is; null for a tree on another volume.
    private fun heldTreeFolderOn(volume: String, isHeld: (String) -> Boolean): String? {
        val treeUri = storageDirectoryRepository.getChosenUri()?.takeIf { it.isNotBlank() } ?: return null
        val folder = grantedTreeOf(treeUri)?.folderOn(volume) ?: return null
        return folder.takeIf { isHeld(treeUri) }
    }

    private fun isHeldAsRemembered(treeUri: String): Boolean = heldTrees.containsKey(treeUri) || isHeldNow(treeUri)

    // Asks the platform and remembers its answer, so a grant it gave up no longer counts when the card is opened.
    private fun isHeldNow(treeUri: String): Boolean {
        val held = isGrantHeld(treeUri)
        if (held) heldTrees[treeUri] = true else heldTrees.remove(treeUri)
        return held
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
