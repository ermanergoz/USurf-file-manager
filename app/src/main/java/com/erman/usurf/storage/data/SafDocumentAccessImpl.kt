package com.erman.usurf.storage.data

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.storage.domain.GrantedTree
import com.erman.usurf.storage.domain.SdCardPath
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.storage.domain.relativeToSdCard
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException

private const val ANY_MIME_TYPE: String = "*/*"

class SafDocumentAccessImpl(
    private val storageDirectoryRepository: StorageDirectoryRepository,
    private val storagePathsProvider: StoragePathsProvider,
    private val openTree: (Uri) -> DocumentFile?
) : SafDocumentAccess {
    override fun documentFor(file: File, isDirectory: Boolean): DocumentFile? = resolveOrNull(file) { root, segments ->
        walkOrCreate(root, segments, isDirectory)
    }

    override fun existingDocumentFor(file: File): DocumentFile? = resolveOrNull(file, ::lookUp)

    private fun resolveOrNull(file: File, walk: (DocumentFile, List<String>) -> DocumentFile?): DocumentFile? = try {
        resolve(file, walk)
    } catch (err: IOException) {
        unresolved(err)
    } catch (err: IllegalArgumentException) {
        unresolved(err)
    } catch (err: SecurityException) {
        unresolved(err)
    }

    private fun resolve(file: File, walk: (DocumentFile, List<String>) -> DocumentFile?): DocumentFile? {
        val treeUri = grantedTreeUri() ?: return null
        val canonicalPath = file.canonicalPath
        val location = sdCardBasePath(treeUri, canonicalPath)
            ?.let { base -> relativeToSdCard(canonicalPath, base) }
            ?: SdCardPath.Outside
        return when (location) {
            SdCardPath.Outside -> null
            SdCardPath.Root -> openSdCardTree(treeUri)
            is SdCardPath.Relative -> openSdCardTree(treeUri)?.let { walk(it, location.segments) }
        }
    }

    private fun grantedTreeUri(): Uri? = storageDirectoryRepository.getChosenUri()
        ?.takeIf { it.isNotBlank() }
        ?.toUri()

    /** The folder the granted tree starts at, when [canonicalPath] lies on the volume that tree belongs to. */
    private fun sdCardBasePath(treeUri: Uri, canonicalPath: String): String? {
        val tree = GrantedTree.fromDocumentId(DocumentsContract.getTreeDocumentId(treeUri))
        val volume = storagePathsProvider.volumeFor(canonicalPath)
        return if (tree == null || volume == null) null else tree.folderOn(volume)
    }

    private fun openSdCardTree(treeUri: Uri): DocumentFile? = openTree(treeUri)

    private fun walkOrCreate(root: DocumentFile, segments: List<String>, isDirectory: Boolean): DocumentFile? =
        segments.foldIndexed<String, DocumentFile?>(root) { index, parent, name ->
            val isFolder = isDirectory || index < segments.lastIndex
            parent?.let { it.findFile(name) ?: createChild(it, name, isFolder) }
        }

    private fun lookUp(root: DocumentFile, segments: List<String>): DocumentFile? =
        segments.fold<String, DocumentFile?>(root) { parent, name -> parent?.findFile(name) }

    private fun createChild(parent: DocumentFile, name: String, isFolder: Boolean): DocumentFile? = if (isFolder) {
        parent.createDirectory(name)
    } else {
        parent.createFile(ANY_MIME_TYPE, name)
    }

    private fun unresolved(err: Exception): DocumentFile? {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        return null
    }
}
