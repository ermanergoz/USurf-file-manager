package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.app.MainApplication.Companion.appContext
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.utils.*
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DirectoryModel {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)
    private val preferenceProvider = PreferenceProvider()

    fun getFileModelsFromFiles(path: String): List<FileModel> {
        val showHidden = preferenceProvider.getShowHiddenPreference()
        var fileList = File(path).listFiles().filter { !it.isHidden || showHidden }.toList()

        when (preferenceProvider.getFileSortPreference()) {
            "Sort by name" -> fileList = fileList.sortedWith(compareBy ({ !it.isDirectory }, { it.name })).toList()
            "Sort by size" -> fileList = fileList.sortedWith(compareBy ({ !it.isDirectory }, { it.length() }, { getFolderSize(it) })).toList()
            "Sort by last modified" -> fileList = fileList.sortedWith(compareBy ({ !it.isDirectory }, { it.lastModified() })).toList()
        }

        if(preferenceProvider.getDescendingOrderPreference())
            fileList = fileList.sortedWith(compareBy { it.isDirectory }).reversed()

        return fileList.map {
            FileModel(it.path, it.name, it.nameWithoutExtension, getConvertedFileSize(it), it.isDirectory,
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
        logd("getDocumentFile")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)
        val getExtSdCardBaseFolder = StoragePaths().getStorageDirectories().elementAt(1)
        var originalDirectory = false
        var relativePathOfFile: String? = null
        try {
            val fullPath = file.canonicalPath
            if (getExtSdCardBaseFolder != fullPath) relativePathOfFile = fullPath.substring(getExtSdCardBaseFolder.length + 1)
            else originalDirectory = true
        } catch (err: IOException) {
            loge("getDocumentFile $err")
            return null
        } catch (err: Exception) {
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
        relativePathOfFile?.split("/".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()?.let { parts ->
            for (i in parts.indices) {
                document?.let {
                    var nextDocument = it.findFile(parts[i])
                    if (nextDocument == null) {
                        nextDocument = if (i < parts.size - 1 || isDirectory) {
                            it.createDirectory(parts[i])
                        } else {
                            it.createFile("*/*", parts[i])
                        }
                    }
                    document = nextDocument
                }
            }
        }
        return document
    }

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        val dirName = File(selectedDirectory.path).parent
        logi("Attempt to rename " + selectedDirectory.path + " to " + newFileName)
        if (selectedDirectory.name == newFileName)
            cancel()

        val prev = File(dirName, selectedDirectory.name)
        val new = File(dirName, newFileName)
        if (!prev.renameTo(new)) {
            val documentFile = getDocumentFile(File(selectedDirectory.path), selectedDirectory.isDirectory)
            documentFile?.renameTo(newFileName)
        }

        if (!File("$dirName/$newFileName").exists())
            cancel()
    }

    suspend fun delete(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        for (i in selectedDirectories.indices) {
            logi("Attempt to delete " + selectedDirectories[i].path)
            val isSuccess = if (selectedDirectories[i].isDirectory) {
                File(selectedDirectories[i].path).deleteRecursively()
            } else {
                File(selectedDirectories[i].path).delete()
            }
            if (!isSuccess) {
                val documentFileToDelete = getDocumentFile(File(selectedDirectories[i].path), selectedDirectories[i].isDirectory)

                if (documentFileToDelete != null && !deleteFolderRecursively(documentFileToDelete))
                    cancel()
            }
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

    suspend fun createFolder(path: String, folderName: String) = withContext(Dispatchers.IO) {
        logi("Attempt to create folder: $path $folderName")
        if (!File("$path/$folderName").exists()) {
            if (!File("$path/$folderName").mkdir()) {
                //try catch wont work here. Doesn't throw IOException
                getDocumentFile(File(path), File(path).isDirectory)?.createDirectory(folderName)
                if (!File("$path/$folderName").exists())
                    cancel()
            }
        } else
            cancel()
    }

    suspend fun createFile(path: String, fileName: String) = withContext(Dispatchers.IO) {
        logi("Attempt to create file: $path $fileName")
        if (!File("$path/$fileName").exists()) {
            try {   //if wont work here. Throws IOException
                File("$path/$fileName").createNewFile()
            } catch (err: Exception) {
                err.printStackTrace()
                getDocumentFile(File(path), File(path).isDirectory)?.createFile("*/*", fileName)
            }
        } else
            cancel()

        if (!File("$path/$fileName").exists())
            cancel()
    }

    private fun doesFileExist(fileModel: FileModel, copyOrMoveDestination: String): Boolean {
        for (file in File(copyOrMoveDestination).listFiles()) {
            if (file.name == fileModel.name)
                return true
        }
        return false
    }

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        for (i in copyOrMoveSources.indices) {
            logi("Attempt to copy: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
            if (!doesFileExist(copyOrMoveSources[i], copyOrMoveDestination)) {
                if (copyOrMoveSources[i].isDirectory) {
                    if (!File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))) {
                        if (!copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination))
                            cancel()
                    }
                } else {
                    try {
                        File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                    } catch (err: IOException) {
                        if (!copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination))
                            cancel()
                    }
                }
            } else
                cancel()
        }
    }

    private fun copyToExtCard(sourceFile: File, copyOrMoveDestination: String): Boolean {
        var documentFileDestination: DocumentFile? = getDocumentFile(File(copyOrMoveDestination), File(copyOrMoveDestination).isDirectory)
        var fileInputStream: FileInputStream? = null
        var outputStream: OutputStream? = null

        if (sourceFile.isDirectory) {
            documentFileDestination?.createDirectory(sourceFile.name)
            val sourceFileList = sourceFile.listFiles()

            sourceFileList?.let {
                for (i in it.indices) {
                    copyToExtCard(it[i],
                        copyOrMoveDestination + File.separator + sourceFile.name)
                }
            }
        } else {
            documentFileDestination?.createFile(sourceFile.extension, sourceFile.name)?.let {
                documentFileDestination = it
            }
            try {
                fileInputStream = FileInputStream(sourceFile)
                outputStream = documentFileDestination?.uri?.let { appContext.contentResolver.openOutputStream(it) }
                val buffer = 6144
                val byteArray = ByteArray(buffer)
                var bytesRead: Int
                try {
                    while (fileInputStream.read(byteArray).also { bytesRead = it } != -1) {
                        outputStream?.write(byteArray, 0, bytesRead)
                    }
                } catch (err: Exception) {
                    loge("copyToExtCard $err")
                } finally {
                    try {
                        fileInputStream.close()
                        outputStream?.close()
                        return true
                    } catch (err: Exception) {
                        loge("copyToExtCard $err")
                    }
                }
            } catch (err: Exception) {
                loge("copyToExtCard $err")
            } finally {
                try {
                    fileInputStream?.close()
                    outputStream?.close()
                    return true
                } catch (err: Exception) {
                    loge("copyToExtCard $err")
                }
            }
            return false
        }
        return false
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        for (i in copyOrMoveSources.indices) {
            logi("Attempt to move: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
            try {
                copyFile(copyOrMoveSources, copyOrMoveDestination)
            } catch (err: CancellationException) {
                cancel()
            }

            try {
                delete(copyOrMoveSources)
            } catch (err: CancellationException) {
                cancel()
            }
        }
    }

    suspend fun compressFile(selectedDirectories: List<FileModel>, zipName: String) = withContext(Dispatchers.IO) {
        val buffer = 6144

        try {
            File(selectedDirectories[0].path).parent?.let { parentDir ->
                val destination = FileOutputStream(parentDir)
                val output = ZipOutputStream(BufferedOutputStream(destination))
                val data = ByteArray(buffer)

                for (i in selectedDirectories.indices) {
                    logi("Attempt to compress: " + selectedDirectories[i].name)
                    if (selectedDirectories[i].isDirectory) {
                        File(selectedDirectories[0].path).listFiles()?.let {
                            if (!zipFolder(it.toList(), output, selectedDirectories[i].name)) {
                                cancel()//in case of an error in zipFolder function
                            }
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
            }
        } catch (err: Exception) {
            loge("compressFile $err")
            cancel()
        }
        true
    }

    private fun zipFolder(selectedDirectories: List<File>, output: ZipOutputStream, folderName: String): Boolean {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            for (i in selectedDirectories.indices) {
                if (selectedDirectories[i].isDirectory) {
                    selectedDirectories[i].listFiles()?.let {
                        zipFolder(it.toList(), output, folderName + File.separator + selectedDirectories[i].name)
                    }
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
            loge("compressFile $err")
            return false
        }
    }

    suspend fun extractFiles(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            for (i in selectedDirectories.indices) {
                logi("Attempt to extract: " + selectedDirectories[i].name)
                File(selectedDirectories[0].path).parent?.let { parentDir ->
                    val baseFolderPath = parentDir + File.separator + File(selectedDirectories[0].path).name
                    File(baseFolderPath).mkdir()

                    val zipInput = ZipInputStream(FileInputStream(selectedDirectories[i].path))
                    var zipContent: ZipEntry

                    while (zipInput.nextEntry.also { zipContent = it } != null) {
                        if (zipContent.name.contains(File.separator)) {    //if it contains directory
                            //zipContent.name -> "someSubFolder/zipContentName.extension"
                            createSubDirectories(
                                zipContent.name,
                                baseFolderPath
                            ) //create all the subdirectories
                        }
                        val fileOutput = FileOutputStream(baseFolderPath + File.separator + zipContent.name)

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
            }
        } catch (err: Exception) {
            loge("compressFile $err")
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