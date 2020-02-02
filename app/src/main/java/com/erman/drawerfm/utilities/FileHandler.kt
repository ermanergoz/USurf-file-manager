package com.erman.drawerfm.utilities

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.erman.drawerfm.R
import java.io.File
import java.io.IOException

fun getFiles(path: String,
             showHidden: Boolean,
             showFilesOnly: Boolean,
             showFoldersOnly: Boolean,
             fileSortMode: String?,
             isAscending: Boolean,
             isDescending: Boolean,
             showFilesOnTop: Boolean,
             showFoldersOnTop: Boolean): List<File> {

    var files = File(path).listFiles().filter { !it.isHidden || showHidden }.filter { it.isFile || !showFilesOnly }
        .filter { it.isDirectory || !showFoldersOnly }.toMutableList()

    if (fileSortMode == "Sort by name") {
        if (isAscending) files =
            files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop }, { !it.isFile || !showFilesOnTop }, { it.name })).toMutableList()
        else if (isDescending) {
            files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
                                                     { !it.isFile || !showFilesOnTop }).thenByDescending { it.name }).toMutableList()
        }
    }

    if (fileSortMode == "Sort by size") {
        if (isAscending) files = files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop },
                                                            { !it.isFile || !showFilesOnTop },
                                                            { it.length() },
                                                            { getFolderSize(it.path) })).toMutableList()
        else if (isDescending) {
            files = files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop },
                                               { !it.isFile || !showFilesOnTop },
                                               { it.length() },
                                               { getFolderSize(it.path) })).asReversed().toMutableList()
            /*asReversed() is a view of the sorted list with reversed index and has better performance than using reverse()*/
            /*in this case we have 2 descending fields and compareByDescending can take only one*/
        }
    }
    if (fileSortMode == "Sort by last modified") {
        if (isAscending) files =
            files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop }, { !it.isFile || !showFilesOnTop }, { it.lastModified() }))
                .toMutableList()
        else if (isDescending) {
            files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
                                                     { !it.isFile || !showFilesOnTop }).thenByDescending { it.lastModified() }).toMutableList()
        }
    }
    return files
}

fun getDocumentFile(file: File, isDirectory: Boolean, context: Context): DocumentFile? {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)

    val getExtSdCardBaseFolder = getStorageDirectories(context).elementAt(1)
    var originalDirectory = false

    var relativePathOfFile: String? = null
    try {
        val fullPath = file.canonicalPath
        if (getExtSdCardBaseFolder != fullPath) relativePathOfFile = fullPath.substring(getExtSdCardBaseFolder!!.length + 1)
        else originalDirectory = true
        Log.e("relativePath", getExtSdCardBaseFolder)
    } catch (e: IOException) {
        return null
    } catch (f: Exception) {
        originalDirectory = true
        //continue
    }

    val extSdCardChosenUri = context.getSharedPreferences("com.erman.draverfm", Context.MODE_PRIVATE).getString("extSdCardChosenUri", null)

    var treeUri: Uri? = null
    if (extSdCardChosenUri != null) treeUri = Uri.parse(extSdCardChosenUri)
    if (treeUri == null) {
        return null
    }

    // start with root of SD card and then parse through document tree.
    var document = DocumentFile.fromTreeUri(context, treeUri)
    if (originalDirectory) return document
    val parts = relativePathOfFile!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    for (i in parts.indices) {
        var nextDocument = document!!.findFile(parts[i])

        if (nextDocument == null) {
            if (i < parts.size - 1 || isDirectory) {
                nextDocument = document.createDirectory(parts[i])
            } else {
                nextDocument = document.createFile("*/*", parts[i])
            }
        }
        document = nextDocument
    }
    return document
}

fun getSearchedFiles(path: String, searchQuery: String): List<File> {
    try {
        return File(path).listFiles(FileSearchFilter(searchQuery)).toList()
    } catch (err: IllegalStateException) {
        Log.e("IllegalStateException", "File(path).listFiles must not be null")
    }
    return emptyList()
}

fun rename(context: Context, selectedDirectories: List<File>, newNameToBe: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {
    var isSuccess = false

    if (isExtSdCard) {
        for (i in selectedDirectories.indices) {
            var newFileName = newNameToBe
            if (i > 0) newFileName = newFileName + "(" + i + ")"
            isSuccess = getDocumentFile(selectedDirectories[i], selectedDirectories[i].isDirectory, context)!!.renameTo(newFileName)
        }
    } else {
        for (i in selectedDirectories.indices) {
            val dirName = selectedDirectories[i].path.removeSuffix(selectedDirectories[i].name)
            var newFileName = newNameToBe

            if (i > 0) newFileName = newFileName + "(" + i + ")"

            if (!selectedDirectories[i].isDirectory) {
                newFileName = newFileName + "." + selectedDirectories[i].extension
            }
            val prev = File(dirName, selectedDirectories[i].name)
            val new = File(dirName, newFileName)
            isSuccess = prev.renameTo(new)

            if (!isSuccess) {
                Toast.makeText(context, context.getString(R.string.error_while_renaming) + prev.name, Toast.LENGTH_LONG).show()
                break
            }
        }
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.renaming_successful), Toast.LENGTH_LONG).show()
        updateFragment.invoke()
    }
}

fun deleteFolderRecursively(documentFile: DocumentFile): Boolean {
    if (documentFile.listFiles().isNotEmpty()) {
        for (i in documentFile.listFiles().size - 1 downTo 0) {
            deleteFolderRecursively(documentFile.listFiles()[i])
        }
    }
    if (documentFile.delete()) return true

    return false
}

fun delete(context: Context, selectedDirectories: List<File>, isExtSdCard: Boolean, updateFragment: () -> Unit) {
    var isSuccess = false

    if (isExtSdCard) {
        for (i in selectedDirectories.indices) {
            isSuccess = deleteFolderRecursively(getDocumentFile(selectedDirectories[i], selectedDirectories[i].isDirectory, context)!!)
        }
    } else {
        for (i in selectedDirectories.indices) {
            isSuccess = if (selectedDirectories[i].isDirectory) {
                File(selectedDirectories[i].path).deleteRecursively()
            } else {
                File(selectedDirectories[i].path).delete()
            }
            if (!isSuccess) {
                Toast.makeText(context, context.getString(R.string.error_while_deleting) + selectedDirectories[i].name, Toast.LENGTH_LONG).show()
                break
            }
        }
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.deleting_successful), Toast.LENGTH_LONG).show()
        updateFragment.invoke()
    }
}

fun createFolder(context: Context, path: String, folderName: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {
    var isSuccess = false
    if (isExtSdCard) {
        getDocumentFile(File(path), File(path).isDirectory, context)!!.createDirectory(folderName)

        if (File(path + "/" + folderName).isDirectory) isSuccess = true
    } else {
        isSuccess = File(path + "/" + folderName).mkdir()
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.folder_creation_successful) + folderName, Toast.LENGTH_LONG).show()

        updateFragment.invoke()
    } else Toast.makeText(context, context.getString(R.string.error_when_creating_folder) + folderName, Toast.LENGTH_LONG).show()
}

fun createFile(context: Context, path: String, folderName: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {
    var isSuccess = false
    if (isExtSdCard) {
        getDocumentFile(File(path), File(path).isDirectory, context)!!.createFile("*/*", folderName)

        if (File(path + "/" + folderName).isFile) isSuccess = true
    } else {
        isSuccess = File(path + "/" + folderName).createNewFile()
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.file_creation_successful) + folderName, Toast.LENGTH_LONG).show()

        updateFragment.invoke()
    } else Toast.makeText(context, context.getString(R.string.error_when_creating_file) + folderName, Toast.LENGTH_LONG).show()
}

fun copyFile(context: Context, copyOrMoveSources: List<File>, copyOrMoveDestination: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {
    var isSuccess = false

    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            isSuccess = File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            try {
                File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
            } catch (err: IOException) {
                isSuccess = false
                break
            }
            isSuccess = true
        }
    }

    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.copy_successful), Toast.LENGTH_LONG).show()
        updateFragment.invoke()
    } else {
        Toast.makeText(context, context.getString(R.string.error_while_copying), Toast.LENGTH_LONG).show()
    }
}

fun moveFile(context: Context, copyOrMoveSources: List<File>, copyOrMoveDestination: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {

    var isSuccess = false

    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            isSuccess = File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            try {
                File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
            } catch (err: IOException) {
                isSuccess = false
                break
            }
            isSuccess = true
        }

        if (copyOrMoveSources[i].isDirectory && isSuccess) {
            isSuccess = File(copyOrMoveSources[i].path).deleteRecursively()
        } else if (!copyOrMoveSources[i].isDirectory && isSuccess) {
            isSuccess = File(copyOrMoveSources[i].path).delete()
        }
    }

    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.moving_successful), Toast.LENGTH_LONG).show()
        updateFragment.invoke()
    } else {
        Toast.makeText(context, context.getString(R.string.error_while_moving), Toast.LENGTH_LONG).show()
    }
}