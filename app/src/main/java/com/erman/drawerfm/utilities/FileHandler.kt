package com.erman.drawerfm.utilities

import com.erman.drawerfm.fragments.ListDirFragment
import java.io.File

fun getFiles(path: String): List<File> {
    return File(path).listFiles().sorted().toList()
}

fun rename(
    selectedDirectories: List<File>,
    newNameToBe: String,
    filesListFragment: ListDirFragment
) {
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
    filesListFragment.updateData()
}

fun delete(selectedDirectories: List<File>, filesListFragment: ListDirFragment) {
    for (i in selectedDirectories.indices) {
        if (selectedDirectories[i].isDirectory) {
            File(selectedDirectories[i].path).deleteRecursively()
        } else {
            File(selectedDirectories[i].path).delete()
        }
    }
    filesListFragment.updateData()
}

fun createFolder(path: String, folderName: String, filesListFragment: ListDirFragment) {
    File(path + "/" + folderName).mkdir()
    filesListFragment.updateData()
}

fun createFile(path: String, folderName: String, filesListFragment: ListDirFragment) {
    File(path + "/" + folderName).createNewFile()
    filesListFragment.updateData()
}

fun copyFile(
    copyOrMoveSources: List<File>,
    copyOrMoveDestination: String,
    filesListFragment: ListDirFragment
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }
    }
    filesListFragment.updateData()
}

fun moveFile(
    copyOrMoveSources: List<File>,
    copyOrMoveDestination: String,
    filesListFragment: ListDirFragment
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isDirectory) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }
    }
    delete(copyOrMoveSources, filesListFragment)
    filesListFragment.updateData()
}