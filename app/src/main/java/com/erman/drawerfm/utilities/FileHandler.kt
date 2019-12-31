package com.erman.drawerfm.utilities

import android.os.FileUtils
import java.io.File


fun getFiles(path: String): List<File> {
    return File(path).listFiles().sorted().toList()
}

fun searchFile(path: String, searchQuery: String): List<File> {
    return File(path).listFiles(FileSearchFilter(searchQuery)).toList()
}

fun rename(selectedDirectories: List<File>, newNameToBe: String, updateFragment: () -> Unit) {
    for (i in selectedDirectories.indices) {
        val dirName = selectedDirectories[i].path.removeSuffix(selectedDirectories[i].name)
        var newFileName = newNameToBe

        if (i > 0)
            newFileName = newFileName + "(" + i + ")"

        if (!selectedDirectories[i].isDirectory) {
            newFileName = newFileName + "." + selectedDirectories[i].extension
        }
        val prev = File(dirName, selectedDirectories[i].name)
        val new = File(dirName, newFileName)

        prev.renameTo(new)
    }
    updateFragment.invoke()
}

fun delete(selectedDirectories: List<File>, updateFragment: () -> Unit) {
    for (i in selectedDirectories.indices) {
        if (selectedDirectories[i].isDirectory) {
            File(selectedDirectories[i].path).deleteRecursively()
        } else {
            File(selectedDirectories[i].path).delete()
        }
    }
    updateFragment.invoke()
}

fun createFolder(path: String, folderName: String, updateFragment: () -> Unit) {
    File(path + "/" + folderName).mkdir()
    updateFragment.invoke()
}

fun createFile(path: String, folderName: String, updateFragment: () -> Unit) {
    File(path + "/" + folderName).createNewFile()
    updateFragment.invoke()
}

fun copyFile(
    copyOrMoveSources: List<File>,
    copyOrMoveDestination: String,
    updateFragment: () -> Unit
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }
    }
    updateFragment.invoke()
}

fun moveFile(
    copyOrMoveSources: List<File>,
    copyOrMoveDestination: String,
    updateFragment: () -> Unit
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }

        if (copyOrMoveSources[i].isDirectory) {
            File(copyOrMoveSources[i].path).deleteRecursively()
        } else {
            File(copyOrMoveSources[i].path).delete()
        }
    }
    updateFragment.invoke()
}