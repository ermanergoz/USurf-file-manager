package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.directory.utils.DUMMY_FILE_NAME
import com.erman.usurf.directory.utils.DUMMY_FILE_NAME_WO_EXTENSION
import com.erman.usurf.directory.utils.ROOT_DIRECTORY
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.utils.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DirectoryModel(private val preferenceProvider: PreferenceProvider,
                     private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
                     private val rootHandler: RootHandler) {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    suspend fun getFileModelsFromFiles(path: String): List<FileModel> = withContext(Dispatchers.IO) {
        val showHidden = preferenceProvider.getShowHiddenPreference()

        File(path).listFiles()?.let { fileList ->
            var filteredFileList = fileList.filter { !it.isHidden || showHidden }.sortedWith(compareBy({ !it.isDirectory }, { it.name })).toList()

            when (preferenceProvider.getFileSortPreference()) {
                "Sort by name" -> filteredFileList = filteredFileList.sortedWith(compareBy({ !it.isDirectory }, { it.name })).toList()
                "Sort by size" -> filteredFileList =
                    filteredFileList.sortedWith(compareBy({ !it.isDirectory }, { it.length() }, { getFolderSize(it) })).toList()
                "Sort by last modified" -> filteredFileList =
                    filteredFileList.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified() })).toList()
            }

            if (preferenceProvider.getDescendingOrderPreference()) filteredFileList =
                filteredFileList.sortedWith(compareBy { it.isDirectory }).reversed()

            return@withContext filteredFileList.map {
                FileModel(it.path,
                    it.name,
                    it.nameWithoutExtension,
                    getConvertedFileSize(it),
                    it.isDirectory,
                    dateFormat.format(it.lastModified()),
                    it.extension,
                    (it.listFiles()?.size.toString() + " files"),
                    getPermissions(it),
                    it.isHidden,
                    false)
            }
        } ?: let {
            //if it is null, it is most likely to be in a root directory
            val rootHandler = RootHandler()
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.getFileList(path).filter { it.first() != '.' || showHidden }.map {
                    FileModel("$path/$it", it, it, "", it.last() == '/', "", "", "", "", it.first() == '.', true)
                }
            } else emptyList()
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
        val size: Long = if (file.isFile) file.length()
        else getFolderSize(file).toLong()

        val sizeStr: String

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
        if (file.exists() && file.parent != ROOT_DIRECTORY) {
            file.listFiles()?.let {
                var size = 0.0
                val fileList = it.toList()

                for (i in fileList.indices) {
                    if (fileList[i].isDirectory) size += getFolderSize(fileList[i])
                    else size += fileList[i].length()
                }
                return size
            }
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

        val extSdCardChosenUri = storageDirectoryPreferenceProvider.getChosenUri()
        var treeUri: Uri? = null
        if (extSdCardChosenUri != null) treeUri = Uri.parse(extSdCardChosenUri)
        if (treeUri == null) {
            return null
        }
        // start with root of SD card and then parse through document tree.
        var document: DocumentFile?
        try {
            document = DocumentFile.fromTreeUri(appContext, treeUri)
        } catch (err: Exception) {
            return null
        }
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

    private suspend fun isRootDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        //check if the directory is in root
        val pathFile = File(path)

        val dummyFilePath = if (pathFile.isDirectory) pathFile.path
        else "" + pathFile.parent

        try {
            withContext(Dispatchers.IO) { File(dummyFilePath + File.separator + DUMMY_FILE_NAME).createNewFile() }
        } catch (err: Exception) {
            err.printStackTrace()
            getDocumentFile(File(dummyFilePath), File(dummyFilePath).isDirectory)?.createFile("*/*", DUMMY_FILE_NAME)
        }
        val isRoot: Boolean = !File(dummyFilePath + File.separator + DUMMY_FILE_NAME).exists()

        if (!isRoot && !File(dummyFilePath + File.separator + DUMMY_FILE_NAME).delete()) {
            val documentFileToDelete = getDocumentFile(File(dummyFilePath + File.separator + DUMMY_FILE_NAME),
                File(dummyFilePath + File.separator + DUMMY_FILE_NAME).isDirectory)
            documentFileToDelete?.let {
                deleteFolderRecursively(it)
            }
        } else if (isRoot) {
            val toDelete = getSearchedDeviceFiles(DUMMY_FILE_NAME_WO_EXTENSION)
            if (toDelete.isNotEmpty()) delete(toDelete)
        }
        Log.e("isRoot", isRoot.toString())
        return@withContext isRoot
    }

    suspend fun rename(selectedDirectory: FileModel, newFileName: String) = withContext(Dispatchers.IO) {
        val dirName = File(selectedDirectory.path).parent
        logi("Attempt to rename " + selectedDirectory.path + " to " + newFileName)
        if (selectedDirectory.name == newFileName) cancel()

        if (isRootDirectory(selectedDirectory.path)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.renameFile(selectedDirectory, newFileName)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
            }
        } else {
            //normal way
            val prev = File(dirName, selectedDirectory.name)
            val new = File(dirName, newFileName)
            if (!prev.renameTo(new)) {
                val documentFile = getDocumentFile(File(selectedDirectory.path), selectedDirectory.isDirectory)
                documentFile?.renameTo(newFileName)
            }
            //if the normal way doesn't work, try with SAF
            if (!File("$dirName/$newFileName").exists()) {
                cancel()
            }
        }
    }

    suspend fun delete(selectedDirectories: List<FileModel>) = withContext(Dispatchers.IO) {
        if (isRootDirectory(selectedDirectories.first().path)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.delete(selectedDirectories)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
            } else cancel()
        } else {
            for (i in selectedDirectories.indices) {
                logi("Attempt to delete " + selectedDirectories[i].path)
                //normal way
                val isSuccess = if (selectedDirectories[i].isDirectory) {
                    File(selectedDirectories[i].path).deleteRecursively()
                } else {
                    File(selectedDirectories[i].path).delete()
                }
                if (!isSuccess) {
                    val documentFileToDelete = getDocumentFile(File(selectedDirectories[i].path), selectedDirectories[i].isDirectory)
                    //if the normal way doesn't work, try with SAF
                    if (documentFileToDelete != null && !deleteFolderRecursively(documentFileToDelete)) cancel()
                }
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

        if (isRootDirectory(path)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.createFolder(path, folderName)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
            } else cancel()
        } else {
            if (!File("$path/$folderName").exists()) {
                //normal way
                //try catch wont work here. Doesn't throw IOException
                if (!File("$path/$folderName").mkdir()) {
                    //if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createDirectory(folderName)
                    if (!File("$path/$folderName").exists()) {
                        cancel()
                    } else Unit
                }
            } else cancel()
        }
    }

    suspend fun createFile(path: String, fileName: String) = withContext(Dispatchers.IO) {
        logi("Attempt to create file: $path $fileName")

        if (isRootDirectory(path)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.createFile(path, fileName)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
                else Unit
            } else cancel()
        } else {
            if (!File("$path/$fileName").exists()) {
                try {//if wont work here. Throws IOException
                    //normal way
                    File("$path/$fileName").createNewFile()
                } catch (err: Exception) {
                    err.printStackTrace()
                    //if the normal way doesn't work, try with SAF
                    getDocumentFile(File(path), File(path).isDirectory)?.createFile("*/*", fileName)
                    if (!File("$path/$fileName").exists()) {
                        cancel()
                    } else Unit
                }
            } else cancel()
        }
    }

    private fun doesFileExist(fileModel: FileModel, copyOrMoveDestination: String): Boolean {
        val files = File(copyOrMoveDestination).listFiles()

        if (files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.name == fileModel.name) return true
            }
        }
        return false
    }

    suspend fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        Log.e("isRootDir", isRootDirectory(copyOrMoveDestination).toString())
        if (isRootDirectory(copyOrMoveDestination)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.copyFile(copyOrMoveSources, copyOrMoveDestination)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
            } else cancel()
        } else {
            for (i in copyOrMoveSources.indices) {
                logi("Attempt to copy: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
                if (!doesFileExist(copyOrMoveSources[i], copyOrMoveDestination)) {
                    //normal way
                    if (copyOrMoveSources[i].isDirectory) {
                        try {
                            File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                        } catch (err: Exception) {
                            if (!copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination)) cancel()
                        }
                    } else {
                        try {
                            File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                        } catch (err: IOException) {
                            //with SAF
                            if (!copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination)) cancel()
                        }
                    }
                } else cancel()
            }
        }
    }

    private fun copyToExtCard(sourceFile: File, copyOrMoveDestination: String): Boolean {
        var documentFileDestination: DocumentFile? = getDocumentFile(File(copyOrMoveDestination), File(copyOrMoveDestination).isDirectory)
        var fileInputStream: FileInputStream? = null
        var outputStream: OutputStream? = null

        if (sourceFile.isDirectory) {
            documentFileDestination?.createDirectory(sourceFile.name)
            var isSuccess = false

            for (i in sourceFile.listFiles()!!.indices) {
                isSuccess = copyToExtCard(sourceFile.listFiles()!![i], copyOrMoveDestination + File.separator + sourceFile.name)
            }
            return isSuccess
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
                } catch (err: Exception) {
                    loge("copyToExtCard $err")
                }
            }
            return false
        }
    }

    suspend fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String) = withContext(Dispatchers.IO) {
        if (isRootDirectory(copyOrMoveDestination)) {
            //do it with root permissions
            if (preferenceProvider.getRootAccessPreference() && rootHandler.isRootAccessGiven()) {
                rootHandler.remountRootDirAs("rw")
                val isSuccess = rootHandler.moveFile(copyOrMoveSources, copyOrMoveDestination)
                rootHandler.remountRootDirAs("ro")
                if (!isSuccess) cancel()
            } else cancel()
        } else {
            for (i in copyOrMoveSources.indices) {
                logi("Attempt to move: from " + copyOrMoveSources[i].path + " to " + copyOrMoveDestination)
            }
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

    @SuppressLint("DefaultLocale")
    suspend fun getSearchedDeviceFiles(searchQuery: String): List<FileModel> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<File>()
        try {
            val storagePaths = StoragePaths().getStorageDirectories()
            for (storagePath in storagePaths) {
                if (storagePath != "/") fileList.addAll(getSubSearchedFiles(File(storagePath), searchQuery))
            }
            return@withContext fileList.filter { file ->
                searchQuery.decapitalize().toRegex().containsMatchIn(file.nameWithoutExtension.decapitalize())
            }.map {
                FileModel(it.path,
                    it.name,
                    it.nameWithoutExtension,
                    getConvertedFileSize(it),
                    it.isDirectory,
                    dateFormat.format(it.lastModified()),
                    it.extension,
                    (it.listFiles()?.size.toString() + " files"),
                    getPermissions(it),
                    it.isHidden)
            }
        } catch (err: java.lang.Exception) {
            loge("getSearchedDeviceFiles $err")
        }
        return@withContext emptyList<FileModel>()
    }

    private fun getSubSearchedFiles(directory: File, searchQuery: String, res: MutableSet<File> = mutableSetOf()): Set<File> {
        //Depth first search algorithm
        directory.listFiles()?.let { fileList ->
            for (file in fileList.toSet()) {
                if (file.isDirectory) {
                    getSubSearchedFiles(file, searchQuery, res)
                } else {
                    res.add(file)
                }
                res.addAll(directory.listFiles()!!.toSet())
            }
        }
        return res
    }

    private fun getInputStream(target: File): InputStream? {
        var destination: InputStream? = null
        target.parent?.let { parentTargetPath ->
            if (File(parentTargetPath).canWrite()) {
                destination = FileInputStream(target)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // if normal way doesn't work, do it with saf
                val targetDocument: DocumentFile = getDocumentFile(target, target.isDirectory) ?: return null
                destination = appContext.contentResolver.openInputStream(targetDocument.uri)
            }
        }
        return destination
    }

    private fun getOutputStream(target: File): OutputStream? {
        var destination: OutputStream? = null
        target.parent?.let { parentTargetPath ->
            if (File(parentTargetPath).canWrite()) {
                destination = FileOutputStream(target)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //saf, same story as input stream
                val targetDocument: DocumentFile = getDocumentFile(target, target.isDirectory) ?: return null
                destination = appContext.contentResolver.openOutputStream(targetDocument.uri)
            }
        }
        return destination
    }

    private suspend fun createSubDirectories(zipEntryName: String, baseFolderPath: String) {
        var subPath = ""

        for (i in zipEntryName.indices) {
            if (zipEntryName[i] != File.separatorChar) subPath += zipEntryName[i]
            else {
                try {
                    subPath += File.separatorChar
                    createFolder(baseFolderPath, subPath)
                    subPath += File.separatorChar
                } catch (err: Exception) {
                }
            }
        }
    }

    private suspend fun cleanLeftover(baseFolderPath: String) {
        val selectedDirectories: MutableList<FileModel> = mutableListOf()
        val files = File(baseFolderPath).listFiles()

        if (files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.isDirectory && file.name[0] == '_') selectedDirectories.add(FileModel(file.path,
                    name = "",
                    nameWithoutExtension = "",
                    size = "",
                    isDirectory = true,
                    lastModified = "",
                    extension = "",
                    subFileCount = "",
                    permission = "",
                    isHidden = false,
                    isInRoot = false,
                    isSelected = false))
            }
        }
        delete(selectedDirectories)
    }

    suspend fun extractFiles(selectedDirectory: FileModel) = withContext(Dispatchers.IO) {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            File(selectedDirectory.path).parent?.let { selectedDirectoryParentPath ->
                val baseFolderPath = selectedDirectoryParentPath + File.separator + File(selectedDirectory.path).nameWithoutExtension

                File(baseFolderPath).parent?.let { baseFolderParentPath ->
                    createFolder(baseFolderParentPath, File(baseFolderPath).nameWithoutExtension)

                    val inputStream = getInputStream(File(selectedDirectory.path))
                    val zipInput = ZipInputStream(BufferedInputStream(inputStream))
                    var zipContent: ZipEntry?

                    while (zipInput.nextEntry.also { zipContent = it } != null) {
                        if (zipContent!!.name.contains(File.separator)) {    //if it contains directory
                            createSubDirectories(zipContent!!.name, baseFolderPath) //create all the subdirectories
                        }
                        val fileOutput = getOutputStream(File(baseFolderPath + File.separator + zipContent!!.name))

                        var counter: Int = zipInput.read(data, 0, buffer)
                        while (counter != -1) {
                            fileOutput?.write(data, 0, counter)
                            counter = zipInput.read(data, 0, buffer)
                        }
                        zipInput.closeEntry()
                        fileOutput?.close()
                    }
                    zipInput.close()
                    try {
                        cleanLeftover(baseFolderPath)
                    } catch (err: Exception) {
                    }
                }
            }
        } catch (err: Exception) {
            loge("extractFiles $err")
            cancel()
        }
    }

    suspend fun compressFiles(zipSources: List<FileModel>, zipNameWithExtension: String) = withContext(Dispatchers.IO) {
        File(zipSources.first().path).parent?.let { zipSourcesParentPath ->
            val outputStream: OutputStream
            val directory = zipSourcesParentPath + File.separator + zipNameWithExtension
            val zipDirectory = File(directory)
            var zipOutputStream: ZipOutputStream? = null
            try {
                outputStream = getOutputStream(zipDirectory)!!
                zipOutputStream = ZipOutputStream(BufferedOutputStream(outputStream))
                for (file in zipSources) {
                    compressFile(File(file.path), "", zipOutputStream)
                }
            } catch (err: IOException) {
                loge("compressFiles $err")
                cancel()
            } finally {
                try {
                    zipOutputStream?.let {
                        zipOutputStream.flush()
                        zipOutputStream.close()
                    }
                } catch (err: IOException) {
                    loge("compressFiles $err")
                    cancel()
                }
            }
        }
    }

    private fun compressFile(file: File, path: String, zipOutputStream: ZipOutputStream?) {
        if (!file.isDirectory) {
            val buffer = 6144
            val data = ByteArray(buffer)
            var len: Int
            val inputStream = BufferedInputStream(FileInputStream(file))
            zipOutputStream!!.putNextEntry(ZipEntry(path + "/" + file.name))
            try {
                while (inputStream.read(data).also { len = it } > 0) {
                    zipOutputStream.write(data, 0, len)
                }
            } catch (err: Exception) {
                loge("compressFile $err")
            } finally {
                inputStream.close()
            }
            return
        }
        if (file.list() == null) return

        file.listFiles()?.let { fileList ->
            for (currentFile in fileList) {
                compressFile(currentFile, path + File.separator + file.name, zipOutputStream)
            }
        }
    }
}