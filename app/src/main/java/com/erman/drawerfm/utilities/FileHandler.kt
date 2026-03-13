package com.erman.drawerfm.utilities

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.erman.drawerfm.R
import java.io.File
import java.io.IOException

fun getFiles(path: String): List<File> {
    return File(path).listFiles().sorted().toList()
}

fun getSearchedFiles(path: String, searchQuery: String): List<File> {
    try {
        return File(path).listFiles(FileSearchFilter(searchQuery)).toList()
    } catch (err: IllegalStateException) {
        Log.e("IllegalStateException", "File(path).listFiles must not be null")
    }
    return emptyList()
}

fun rename(context: Context, selectedDirectories: List<File>, newNameToBe: String, updateFragment: () -> Unit) {
    var isSuccess = false

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
            Toast.makeText(context, context.getString(R.string.error_while_renaming) + prev.name, Toast.LENGTH_LONG)
                .show()
            break
        }
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.renaming_successful), Toast.LENGTH_LONG).show()

        updateFragment.invoke()
    }
}

fun delete(context: Context, selectedDirectories: List<File>, updateFragment: () -> Unit) {
    var isSuccess = false

    for (i in selectedDirectories.indices) {
        isSuccess = if (selectedDirectories[i].isDirectory) {
            File(selectedDirectories[i].path).deleteRecursively()
        } else {
            File(selectedDirectories[i].path).delete()
        }

        if (!isSuccess) {
            Toast.makeText(context,
                           context.getString(R.string.error_while_deleting) + selectedDirectories[i].name,
                           Toast.LENGTH_LONG).show()
            break
        }
    }
    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.deleting_successful), Toast.LENGTH_LONG).show()
        updateFragment.invoke()
    }
}

fun createFolder(context: Context, path: String, folderName: String, updateFragment: () -> Unit) {
    var isSuccess = File(path + "/" + folderName).mkdir()

    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.folder_creation_successful) + folderName, Toast.LENGTH_LONG)
            .show()

        updateFragment.invoke()
    } else Toast.makeText(context,
                          context.getString(R.string.error_when_creating_folder) + folderName,
                          Toast.LENGTH_LONG).show()
}

fun createFile(context: Context, path: String, folderName: String, updateFragment: () -> Unit) {
    val isSuccess = File(path + "/" + folderName).createNewFile()

    if (isSuccess) {
        Toast.makeText(context, context.getString(R.string.file_creation_successful) + folderName, Toast.LENGTH_LONG)
            .show()

        updateFragment.invoke()
    } else Toast.makeText(context,
                          context.getString(R.string.error_when_creating_file) + folderName,
                          Toast.LENGTH_LONG).show()
}

fun copyFile(context: Context,
             copyOrMoveSources: List<File>,
             copyOrMoveDestination: String,
             updateFragment: () -> Unit) {
    var isSuccess = false

    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            isSuccess =
                File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
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

fun moveFile(context: Context,
             copyOrMoveSources: List<File>,
             copyOrMoveDestination: String,
             updateFragment: () -> Unit) {

    var isSuccess = false

    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            isSuccess =
                File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
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