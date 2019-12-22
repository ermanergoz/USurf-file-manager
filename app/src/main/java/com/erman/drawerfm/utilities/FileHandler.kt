import android.util.Log
import com.erman.drawerfm.fragments.ListDirFragment
import java.io.File
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
    selectedDirectories: List<DirectoryData>,
    newNameToBe: String,
    filesListFragment: ListDirFragment
) {
    for (i in selectedDirectories.indices) {
        val dirName = selectedDirectories[i].path.removeSuffix(selectedDirectories[i].name)
        var newFileName = newNameToBe

        if (i > 0)
            newFileName = newFileName + "(" + i + ")"

        if (!selectedDirectories[i].isFolder) {
            newFileName = newFileName + "." + selectedDirectories[i].extension
        }
        val prev = File(dirName, selectedDirectories[i].name)
        val new = File(dirName, newFileName)

        prev.renameTo(new)
    }
    filesListFragment.updateData()
}

fun delete(selectedDirectories: List<DirectoryData>, filesListFragment: ListDirFragment) {
    for (i in selectedDirectories.indices) {
        if (selectedDirectories[i].isFolder) {
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
    copyOrMoveSources: List<DirectoryData>,
    copyOrMoveDestination: String,
    filesListFragment: ListDirFragment
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isFolder) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }
    }
    filesListFragment.updateData()
}

fun moveFile(
    copyOrMoveSources: List<DirectoryData>,
    copyOrMoveDestination: String,
    filesListFragment: ListDirFragment
) {
    for (i in copyOrMoveSources.indices) {
        if (copyOrMoveSources[i].isFolder) {
            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + copyOrMoveSources[i].name))
        } else {
            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + "/" + copyOrMoveSources[i].name))
        }
    }
    delete(copyOrMoveSources, filesListFragment)
    filesListFragment.updateData()
}