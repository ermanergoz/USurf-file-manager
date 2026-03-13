package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.utils.DirectoryPreferenceProvider
import com.erman.usurf.utils.StoragePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DirectoryModel {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    fun getFileModelsFromFiles(path: String): List<FileModel> {
        val files = File(path).listFiles().toList()
        return files.map {
            FileModel(it.path, it.nameWithoutExtension, getConvertedFileSize(it), it.isDirectory,
                dateFormat.format(it.lastModified()), it.extension,
                (it.listFiles()?.size.toString() + " files"), getPermissions(it), it.isHidden)
        }
    }

    private fun getPermissions(file: File): String {
        return when {
            file.canRead() && file.canWrite() -> "-RW"
            else -> if (!file.canRead() && file.canWrite()) "-W"
            else if (file.canRead() && !file.canWrite()) "-R"
            else "NONE"
        }
    }

    private fun getConvertedFileSize(file: File): String {
        val size: Long = if (file.isFile)
            file.length()
        else getFolderSize(file).toLong()

        var sizeStr = ""

        val kilobyte = size / 1024.0
        val megabyte = size / (1024.0 * 1024.0)
        val gigabyte = size / (1024.0 * 1024.0 * 1024.0)
        val terabyte = size / (1024.0 * 1024.0 * 1024.0 * 1024.0)

        sizeStr = when {
            terabyte > 1 -> "%.2f".format(terabyte) + " TB"
            gigabyte > 1 -> "%.2f".format(gigabyte) + " GB"
            megabyte > 1 -> "%.2f".format(megabyte) + " MB"
            kilobyte > 1 -> "%.2f".format(kilobyte) + " KB"
            else -> "$size Bytes"
        }
        return sizeStr
    }

    private fun getFolderSize(file: File): Double {
        if (file.exists() && file.listFiles() != null) {
            var size = 0.0
            val fileList = file.listFiles().toList()

            for (i in fileList.indices) {
                if (fileList[i].isDirectory)
                    size += getFolderSize(fileList[i])
                else size += fileList[i].length()
            }
            return size
        }
        return 0.0
    }

    fun manageMultipleSelectionList(file: FileModel, multipleSelection: MutableList<FileModel>): MutableList<FileModel> {
        if (file.isSelected) {
            file.isSelected = false
            multipleSelection.remove(file)
        } else {
            file.isSelected = true
            multipleSelection.add(file)
        }
        return multipleSelection
    }

    fun clearMultipleSelection(multipleSelection: MutableList<FileModel>): MutableList<FileModel> {
        for (i in multipleSelection.indices) {
            multipleSelection[i].isSelected = false
        }
        multipleSelection.clear()
        return multipleSelection
    }

    private fun getDocumentFile(file: File, isDirectory: Boolean): DocumentFile? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)
        val getExtSdCardBaseFolder = StoragePaths().getStorageDirectories().elementAt(1)
        var originalDirectory = false
        var relativePathOfFile: String? = null
        try {
            val fullPath = file.canonicalPath
            if (getExtSdCardBaseFolder != fullPath) relativePathOfFile = fullPath.substring(getExtSdCardBaseFolder.length + 1)
            else originalDirectory = true
        } catch (e: IOException) {
            return null
        } catch (f: Exception) {
            originalDirectory = true
        }
        val extSdCardChosenUri = DirectoryPreferenceProvider().getChosenUri()
        var treeUri: Uri? = null
        if (extSdCardChosenUri != null) treeUri = Uri.parse(extSdCardChosenUri)
        if (treeUri == null) {
            return null
        }
        // start with root of SD card and then parse through document tree.
        var document = DocumentFile.fromTreeUri(appContext, treeUri)
        if (originalDirectory) return document
        val parts = relativePathOfFile!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in parts.indices) {
            var nextDocument = document!!.findFile(parts[i])
            if (nextDocument == null) {
                nextDocument = if (i < parts.size - 1 || isDirectory) {
                    document.createDirectory(parts[i])
                } else {
                    document.createFile("*/*", parts[i])
                }
            }
            document = nextDocument
        }
        return document
    }

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        val dirName = File(selectedDirectory.path).parent

        val prevFullName = if (selectedDirectory.isDirectory) {
            selectedDirectory.name
        } else {
            selectedDirectory.name + "." + selectedDirectory.extension
        }

        if (prevFullName == newFileName)
            cancel()

        val prev = File(dirName, prevFullName)
        val new = File(dirName, newFileName)
        prev.renameTo(new)

        if (!File("$dirName$newFileName").exists())
            cancel()
    }

    suspend fun renameWithSaf(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        val dirName = File(selectedDirectory.path).parent
        val documentFile = getDocumentFile(File(selectedDirectory.path), selectedDirectory.isDirectory)
        documentFile?.renameTo(newFileName)

        if (!File("$dirName/$newFileName").exists())
            cancel()
    }

    suspend fun delete(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        for (i in selectedDirectories.indices) {
            val isSuccess = if (selectedDirectories[i].isDirectory) {
                File(selectedDirectories[i].path).deleteRecursively()
            } else {
                File(selectedDirectories[i].path).delete()
            }
            if (!isSuccess)
                cancel()
        }
    }

    private fun deleteFolderRecursively(documentFile: DocumentFile): Boolean {
        if (documentFile.listFiles().isNotEmpty()) {
            for (i in documentFile.listFiles().size - 1 downTo 0) {
                deleteFolderRecursively(documentFile.listFiles()[i])
            }
        }
        documentFile.delete()
        return !documentFile.exists()
    }

    suspend fun deleteWithSaf(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        for (i in selectedDirectories.indices) {
            if (!deleteFolderRecursively(getDocumentFile(File(selectedDirectories[i].path), selectedDirectories[i].isDirectory)!!)) {
                cancel()
            }
        }
    }

    suspend fun createFolder(path: String, folderName: String) = withContext(Dispatchers.IO) {
        if (!File("$path/$folderName").exists()) {
            File("$path/$folderName").mkdir()
            if (!File("$path/$folderName").exists())   //try catch wont work here. Doesnt throw IOException
                cancel()
        } else
            cancel()
    }

    suspend fun createFolderWithSaf(path: String, folderName: String) = withContext(Dispatchers.IO) {
        if (!File("$path/$folderName").exists()) {
            getDocumentFile(File(path), File(path).isDirectory)!!.createDirectory(folderName)
            if (!File("$path/$folderName").exists())
                cancel()
        } else {
            cancel()
        }
    }

    suspend fun createFile(path: String, fileName: String) = withContext(Dispatchers.IO) {
        if (!File("$path/$fileName").exists()) {
            try {   ////if wont work here. Throws IOException
                File("$path/$fileName").createNewFile()
            } catch (err: IOException) {
                cancel()
            }
        } else
            cancel()
    }

    suspend fun createFileWithSaf(path: String, fileName: String) = withContext(Dispatchers.IO) {
        if (!File("$path/$fileName").exists()) {
            getDocumentFile(File(path), File(path).isDirectory)!!.createFile("*/*", fileName)
            if (!File("$path/$fileName").exists())
                cancel()
        } else
            cancel()
    }

    private fun doesFileExist(fileModel: FileModel, copyOrMoveDestination: String): Boolean {
        for (file in File(copyOrMoveDestination).listFiles()) {
            if (file.name == fileModel.name + "." + fileModel.extension)
                return true
        }
        return false
    }

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        for (i in copyOrMoveSources.indices) {
            if (!doesFileExist(copyOrMoveSources[i], copyOrMoveDestination)) {
                if (copyOrMoveSources[i].isDirectory) {
                    if (!File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name)))
                        cancel()
                } else {
                    try {
                        File(copyOrMoveSources[i].path).copyTo(File(
                            copyOrMoveDestination + File.separator + copyOrMoveSources[i].name + "." + copyOrMoveSources[i].extension))
                    } catch (err: IOException) {
                        cancel()
                    }
                }
            } else {
                Log.e("file already", "exists")
                cancel()
            }
        }
    }

    suspend fun copyFileWithSaf(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        for (i in copyOrMoveSources.indices) {
            if (!doesFileExist(copyOrMoveSources[i], copyOrMoveDestination)) {
                if (!copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination))
                    cancel()
            } else {
                Log.e("file already", "exists")
                cancel()
            }
        }
    }

    private fun copyToExtCard(sourceFile: File, copyOrMoveDestination: String?): Boolean {
        var documentFileDestination: DocumentFile = getDocumentFile(
            File(copyOrMoveDestination!!),
            File(copyOrMoveDestination).isDirectory
        )!!
        var fileInputStream: FileInputStream? = null
        var outputStream: OutputStream? = null

        if (sourceFile.isDirectory) {
            documentFileDestination.createDirectory(sourceFile.name)!!

            for (i in sourceFile.listFiles()!!.indices) {
                copyToExtCard(sourceFile.listFiles()!![i],
                    copyOrMoveDestination + File.separator + sourceFile.name)
            }
        } else {
            documentFileDestination = documentFileDestination.createFile(sourceFile.extension, sourceFile.name)!!
            try {
                fileInputStream = FileInputStream(sourceFile)
                outputStream = appContext.contentResolver.openOutputStream(documentFileDestination.uri)!!
                val buffer = 6144
                val byteArray = ByteArray(buffer)
                var bytesRead: Int
                try {
                    while (fileInputStream.read(byteArray).also { bytesRead = it } != -1) {
                        outputStream.write(byteArray, 0, bytesRead)
                    }
                } catch (err: Exception) {
                    err.printStackTrace()
                } finally {
                    try {
                        fileInputStream.close()
                        outputStream.close()
                        return true
                    } catch (err: Exception) {
                        err.printStackTrace()
                    }
                }
            } catch (err: Exception) {
                err.printStackTrace()
            } finally {
                try {
                    fileInputStream!!.close()
                    outputStream!!.close()
                    return true
                } catch (err: Exception) {
                    err.printStackTrace()
                }
            }
            return false
        }
        return false
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        for (i in copyOrMoveSources.indices) {
            if (copyOrMoveSources[i].isDirectory) {
                if (!File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name)))
                    cancel()
            } else {
                try {
                    File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                } catch (err: IOException) {
                    cancel()
                }
            }
            if (copyOrMoveSources[i].isDirectory) {
                if (!File(copyOrMoveSources[i].path).deleteRecursively())
                    cancel()
            } else if (!copyOrMoveSources[i].isDirectory) {
                if (!File(copyOrMoveSources[i].path).delete())
                    cancel()
            }
        }
    }

    suspend fun moveFileWithSaf(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        try {
            copyFileWithSaf(copyOrMoveSources, copyOrMoveDestination)
        } catch (err: Exception) {
            cancel()
        }

        try {
            deleteWithSaf(copyOrMoveSources)
        } catch (err: Exception) {
            cancel()
        }
    }

    suspend fun compressFile(selectedDirectories: List<FileModel>, zipName: String) = withContext(Dispatchers.IO) {
        val buffer = 6144

        try {
            val destination = FileOutputStream(File(selectedDirectories[0].path).parent!! + File.separator + zipName + ".zip") //burası
            val output = ZipOutputStream(BufferedOutputStream(destination))
            val data = ByteArray(buffer)

            for (i in selectedDirectories.indices) {
                if (selectedDirectories[i].isDirectory) {
                    if (!zipFolder(File(selectedDirectories[0].path).listFiles()!!.toList(), output, selectedDirectories[i].name)) {
                        cancel()//in case of an error in zipFolder function
                    }
                } else {
                    val fileOrigin = BufferedInputStream(FileInputStream(File(selectedDirectories[0].path)))
                    output.putNextEntry(ZipEntry(selectedDirectories[i].name))
                    var counter = (fileOrigin.read(data, 0, buffer))

                    while (counter != -1) {
                        output.write(data, 0, counter)
                        counter = (fileOrigin.read(data, 0, buffer))
                    }
                    fileOrigin.close()
                }
            }
            output.close()
        } catch (err: Exception) {
            err.printStackTrace()
            cancel(err.toString())
        }
        true
    }

    private fun zipFolder(selectedDirectories: List<File>, output: ZipOutputStream, folderName: String): Boolean {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            for (i in selectedDirectories.indices) {
                if (selectedDirectories[i].isDirectory) {
                    zipFolder(
                        selectedDirectories[i].listFiles()!!.toList(),
                        output,
                        folderName + File.separator + selectedDirectories[i].name
                    )
                } else {
                    output.putNextEntry(ZipEntry(folderName + File.separator + selectedDirectories[i].name))
                    val fileOrigin = BufferedInputStream(FileInputStream(selectedDirectories[i]))

                    var counter = (fileOrigin.read(data, 0, buffer))
                    while (counter != -1) {
                        output.write(data, 0, counter)
                        counter = (fileOrigin.read(data, 0, buffer))
                    }
                    fileOrigin.close()
                }
            }
            return true
        } catch (err: Exception) {
            Log.e("Error while compressing", err.toString())
            return false
        }
    }

    suspend fun extractFiles(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            for (i in selectedDirectories.indices) {
                val baseFolderPath =
                    File(selectedDirectories[0].path).parent!! + File.separator + File(selectedDirectories[0].path).nameWithoutExtension
                File(baseFolderPath).mkdir()

                val zipInput = ZipInputStream(FileInputStream(selectedDirectories[i].path))
                var zipContent: ZipEntry?

                while (zipInput.nextEntry.also { zipContent = it } != null) {
                    if (zipContent!!.name.contains(File.separator)) {    //if it contains directory
                        //zipContent!!.name -> "someSubFolder/zipContentName.extension"
                        createSubDirectories(
                            zipContent!!.name,
                            baseFolderPath
                        ) //create all the subdirectories
                    }
                    val fileOutput = FileOutputStream(baseFolderPath + File.separator + zipContent!!.name)

                    var counter: Int = zipInput.read(data, 0, buffer)
                    while (counter != -1) {
                        fileOutput.write(data, 0, counter)
                        counter = zipInput.read(data, 0, buffer)
                    }
                    zipInput.closeEntry()
                    fileOutput.close()
                }
                zipInput.close()
            }
        } catch (err: Exception) {
            err.printStackTrace()
            cancel()
        }
    }

    private fun createSubDirectories(zipEntryName: String, baseFolderPath: String) {
        var subPath = ""

        for (i in zipEntryName.indices) {
            subPath += if (zipEntryName[i] != '/') zipEntryName[i]
            else {
                File(baseFolderPath + File.separator + subPath).mkdir()
                '/'
            }
        }
    }
}