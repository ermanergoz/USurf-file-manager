import java.io.File
import java.util.*


fun getFiles(path: String): List<File> {
    return File(path).listFiles().sorted().toList()
}

fun getDirectoryData(files: List<File>): List<DirectoryData> {

    var fileModelList: MutableList<DirectoryData> = mutableListOf()

    for (i in 0 until files.size) {
        fileModelList.add(
            DirectoryData(
                files[i].path,
                files[i].isDirectory,
                files[i].name,
                convertFileSizeToMB(files[i].length())/*, it.extension*/,
                files[i].listFiles()?.size
                    ?: 0,
                Date(files[i].lastModified())
            )
        )
    }
    return fileModelList
}


fun convertFileSizeToMB(sizeInBytes: Long): Double {
    return (sizeInBytes.toDouble()) / (1024 * 1024)
    //TODO: Use the nice function in StorageUsageData class instead and delete this
}

