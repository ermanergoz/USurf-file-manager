import android.content.Intent
import android.util.Log
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.ListDirFragment
import java.io.File
import java.io.IOException
import java.util.*

fun getFiles(path: String): List<File> {
    Log.e("current path", path)
    return File(path).listFiles().sorted().toList()
}

fun getDirectoryData(files: List<File>): List<DirectoryData> {

    val fileModelList: MutableList<DirectoryData> = mutableListOf()

    for (i in files.indices) {
        fileModelList.add(
            DirectoryData(
                files[i].path,
                files[i].isDirectory,
                files[i].name,
                convertFileSizeToMB(files[i].length()),
                files[i].listFiles()?.size ?: 0,
                Date(files[i].lastModified()),
                files[i].extension
            )
        )
    }
    Log.e("dir size", fileModelList.size.toString())
    return fileModelList
}

fun convertFileSizeToMB(sizeInBytes: Long): Double {
    return (sizeInBytes.toDouble()) / (1024 * 1024)
    //TODO: Use the nice function in StorageUsageData class instead and delete this
}

fun rename(
    selectedDirectory: DirectoryData,
    newNameToBe: String,
    filesListFragment: ListDirFragment
) {
    var dirName = selectedDirectory.path.removeSuffix(selectedDirectory.name)
    var newFileName = newNameToBe

    if (!selectedDirectory.isFolder) {
        newFileName = newNameToBe + "." + selectedDirectory.extension
    }
    var prev = File(dirName, selectedDirectory.name)
    var new = File(dirName, newFileName)

    prev.renameTo(new)

    filesListFragment.updateData()
}

fun delete(selectedDirectory: DirectoryData, filesListFragment: ListDirFragment) {
    if (selectedDirectory.isFolder) {
        File(selectedDirectory.path).deleteRecursively()
    } else {
        File(selectedDirectory.path).delete()
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

fun copyFile(copyOrMoveSource: DirectoryData, copyOrMoveDestination: String, filesListFragment: ListDirFragment) {
    if (copyOrMoveSource.isFolder) {
        File(copyOrMoveSource.path).copyRecursively(File(copyOrMoveDestination+copyOrMoveSource.name))
    } else {
        File(copyOrMoveSource.path).copyTo(File(copyOrMoveDestination+"/"+copyOrMoveSource.name))
    }

    filesListFragment.updateData()
}

fun moveFile(copyOrMoveSource: DirectoryData, copyOrMoveDestination: String, filesListFragment: ListDirFragment) {
    if (copyOrMoveSource.isFolder) {
        File(copyOrMoveSource.path).copyRecursively(File(copyOrMoveDestination+copyOrMoveSource.name))
    } else {
        File(copyOrMoveSource.path).copyTo(File(copyOrMoveDestination+"/"+copyOrMoveSource.name))
    }
    filesListFragment.updateData()

    delete(copyOrMoveSource, filesListFragment)
}