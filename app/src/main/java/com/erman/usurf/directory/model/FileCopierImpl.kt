package com.erman.usurf.directory.model

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import java.io.IOException
import java.io.OutputStream

private const val BUFFER_SIZE = 131072 // Ideal for most devices also considering the old devices

class FileCopierImpl(private val safDocumentAccess: SafDocumentAccess, private val openOutput: (Uri) -> OutputStream?) : FileCopier {
    override fun copy(source: File, destinationFolder: File): CopiedItem {
        val target = File(destinationFolder, source.name)
        // Checked first, so whatever is at the target after a failed copy was made by that copy and may be removed.
        if (target.exists() || destinationFolder.listFiles()?.any { it.name == source.name } == true) {
            throw FileOperationException("Destination already contains an item with this name", OperationError.AlreadyExists)
        }
        if (copyWithFileApi(source, target)) return CopiedFile(target)
        target.deleteRecursively()
        return copyIntoTree(source, destinationFolder)?.let(::CopiedDocument) ?: throw FileOperationException("Copy failed")
    }

    // copyRecursively also copies a single file, and ignores a folder it could not create, so the target must exist after it.
    private fun copyWithFileApi(source: File, target: File): Boolean = try {
        source.copyRecursively(target) && target.exists()
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    }

    /** Copies through the granted SD card tree; a failed copy deletes the document it created, which frees its space. */
    private fun copyIntoTree(source: File, destinationFolder: File): DocumentFile? {
        val folder = safDocumentAccess.existingDocumentFor(destinationFolder) ?: return null
        val copy = createIn(folder, source) ?: return null
        if (writeTree(source, copy)) return copy
        copy.delete()
        return null
    }

    private fun createIn(folder: DocumentFile, source: File): DocumentFile? = if (source.isDirectory) {
        folder.createDirectory(source.name)
    } else {
        folder.createFile(source.extension, source.name)
    }

    // Stops at the first child that fails, so no later success can hide it.
    private fun writeTree(source: File, document: DocumentFile): Boolean = if (source.isDirectory) {
        source.listFiles()?.all { child -> createIn(document, child)?.let { writeTree(child, it) } == true } ?: false
    } else {
        writeFileToDocumentFile(source, document)
    }

    private fun writeFileToDocumentFile(sourceFile: File, documentFile: DocumentFile): Boolean = try {
        sourceFile.inputStream().use { input ->
            val output = openOutput(documentFile.uri) ?: throw IOException("No output stream for ${documentFile.uri}")
            output.use { input.copyTo(it, BUFFER_SIZE) }
        }
        true
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    } catch (err: SecurityException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    }
}

private class CopiedFile(private val file: File) : CopiedItem {
    override fun shape(): EntryShape? = EntryShape.of(file)

    override fun remove(): Boolean = file.deleteRecursively()
}

/** A copy in the SD card tree, read back through the document provider that wrote it. */
private class CopiedDocument(private val document: DocumentFile) : CopiedItem {
    override fun shape(): EntryShape? = if (document.exists()) {
        shapeOf(document, DocumentFile::getName, DocumentFile::isDirectory, DocumentFile::length) { it.listFiles().toList() }
    } else {
        null
    }

    override fun remove(): Boolean = document.delete() || !document.exists()
}
