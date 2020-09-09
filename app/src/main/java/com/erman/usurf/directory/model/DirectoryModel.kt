package com.erman.usurf.directory.model

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.MainApplication.Companion.appContext
import com.erman.usurf.R
import com.erman.usurf.directory.utils.SIMPLE_DATE_FORMAT_PATTERN
import com.erman.usurf.utils.DirectoryPreferenceProvider
import com.erman.usurf.utils.StoragePaths
import java.io.*
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DirectoryModel {
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    fun getFileModelsFromFiles(path: String): List<FileModel> {
        Log.e("current path", path)
        val files = File(path).listFiles().toList()
        return files.map {
            FileModel(it.path, it.name, getConvertedFileSize(it), it.isDirectory,
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
        if (file.exists()) {
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

    fun rename(selectedDirectories: List<FileModel>, newNameToBe: String): Boolean {
        var isSuccess = false

        for (i in selectedDirectories.indices) {
            val dirName = selectedDirectories[i].path.removeSuffix(selectedDirectories[i].name)
            var newFileName = newNameToBe

            if (i > 0) newFileName = "$newFileName($i)"

            if (!selectedDirectories[i].isDirectory) {
                newFileName = newFileName + "." + selectedDirectories[i].extension
            }
            val prev = File(dirName, selectedDirectories[i].name)
            val new = File(dirName, newFileName)
            isSuccess = prev.renameTo(new)

        }
        return isSuccess
    }

    fun renameWithSaf(selectedDirectories: List<FileModel>, newNameToBe: String): Boolean {
        var isSuccess = false

        for (i in selectedDirectories.indices) {
            var newFileName = newNameToBe
            if (i > 0) newFileName = "$newFileName($i)"
            isSuccess = getDocumentFile(File(selectedDirectories[i].path),
                selectedDirectories[i].isDirectory)!!.renameTo(newFileName)
        }
        return isSuccess
    }

    fun delete(selectedDirectories: List<FileModel>): Boolean {
        var isSuccess = false

        for (i in selectedDirectories.indices) {
            isSuccess = if (selectedDirectories[i].isDirectory) {
                File(selectedDirectories[i].path).deleteRecursively()
            } else {
                File(selectedDirectories[i].path).delete()
            }
            if (!isSuccess)
                break
        }
        return isSuccess
    }

    private fun deleteFolderRecursively(documentFile: DocumentFile): Boolean {
        if (documentFile.listFiles().isNotEmpty()) {
            for (i in documentFile.listFiles().size - 1 downTo 0) {
                deleteFolderRecursively(documentFile.listFiles()[i])
            }
        }
        if (documentFile.delete()) return true
        return false
    }

    fun deleteWithSaf(selectedDirectories: List<FileModel>): Boolean {
        var isSuccess = false

        for (i in selectedDirectories.indices) {
            isSuccess = deleteFolderRecursively(getDocumentFile(
                File(selectedDirectories[i].path), selectedDirectories[i].isDirectory)!!)
            if (!isSuccess)
                break
        }
        return isSuccess
    }

    fun createFolder(path: String, folderName: String): Boolean {
        return try {
            if (!File("$path/$folderName").exists())
                File("$path/$folderName").mkdir()
            else
                false
        } catch (err: IOException) {
            err.printStackTrace()
            false
        }
    }

    fun createFolderWithSaf(path: String, folderName: String): Boolean {
        return try {
            if (!File("$path/$folderName").exists()) {
                getDocumentFile(File(path), File(path).isDirectory)!!.createDirectory(folderName)
                File("$path/$folderName").exists()
            } else
                false
        } catch (err: NullPointerException) {
            err.printStackTrace()
            false
        }
    }

    fun createFile(path: String, fileName: String): Boolean {
        return try {
            if (!File("$path/$fileName").exists())
                File("$path/$fileName").createNewFile()
            else
                false
        } catch (err: IOException) {
            err.printStackTrace()
            false
        }
    }

    fun createFileWithSaf(path: String, fileName: String): Boolean {
        return try {
            if (!File("$path/$fileName").exists()) {
                getDocumentFile(File(path), File(path).isDirectory)!!.createFile("*/*", fileName)
                File("$path/$fileName").exists()
            } else
                false
        } catch (err: NullPointerException) {
            err.printStackTrace()
            false
        }
    }

    fun copyFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String): Boolean {
        var isSuccess = false

        for (i in copyOrMoveSources.indices) {
            if (copyOrMoveSources[i].isDirectory) {
                isSuccess =
                    File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
            } else {
                try {
                    File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
                } catch (err: IOException) {
                    isSuccess = false
                    break
                }
                isSuccess = true
            }
        }
        return isSuccess
    }

    fun copyFileWithSaf(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String): Boolean {
        var isSuccess = false
        for (i in copyOrMoveSources.indices) {
            isSuccess = copyToExtCard(File(copyOrMoveSources[i].path), copyOrMoveDestination)
            if (!isSuccess) break
        }
        return isSuccess
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

    fun moveFile(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String): Boolean {
        var isSuccess = false

        for (i in copyOrMoveSources.indices) {
            if (copyOrMoveSources[i].isDirectory) {
                isSuccess = File(copyOrMoveSources[i].path).copyRecursively(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
            } else {
                try {
                    File(copyOrMoveSources[i].path).copyTo(File(copyOrMoveDestination + File.separator + copyOrMoveSources[i].name))
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
        return isSuccess
    }

    fun moveFileWithSaf(copyOrMoveSources: List<FileModel>, copyOrMoveDestination: String): Boolean {
        if (copyFileWithSaf(copyOrMoveSources, copyOrMoveDestination))
            return deleteWithSaf(copyOrMoveSources)
        return false
    }

    fun zipFile(context: Context, selectedDirectories: List<File>, zipName: String, isExtSdCard: Boolean, updateFragment: () -> Unit) {
        val buffer = 6144

        try {
            val destination = FileOutputStream(selectedDirectories[0].parent!! + File.separator + zipName + ".zip") //burası
            val output = ZipOutputStream(BufferedOutputStream(destination))
            val data = ByteArray(buffer)

            for (i in selectedDirectories.indices) {
                if (selectedDirectories[i].isDirectory) {
                    if (!zipFolder(
                            selectedDirectories[i].listFiles()!!.toList(),
                            output,
                            selectedDirectories[i].name
                        )
                    ) {
                        Toast.makeText(context, context.getString(R.string.error_while_compressing), Toast.LENGTH_LONG).show()
                        return  //in case of an error in zipFolder function
                    }
                } else {
                    val fileOrigin = BufferedInputStream(FileInputStream(selectedDirectories[i]))
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
            Toast.makeText(context, context.getString(R.string.error_while_compressing), Toast.LENGTH_LONG).show()
            Log.e("Error while compressing", err.toString())
            return
        }
        updateFragment.invoke()
        Toast.makeText(context, context.getString(R.string.compressing_successful), Toast.LENGTH_LONG).show()
    }

    fun zipFolder(selectedDirectories: List<File>, output: ZipOutputStream, folderName: String): Boolean {
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

    fun unzip(context: Context, selectedDirectories: List<File>, updateFragment: () -> Unit) {
        val buffer = 6144
        val data = ByteArray(buffer)

        try {
            for (i in selectedDirectories.indices) {
                val baseFolderPath = selectedDirectories[i].parent!! + File.separator + selectedDirectories[i].nameWithoutExtension
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
            updateFragment.invoke()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_while_extracting), Toast.LENGTH_LONG).show()
            Log.e("Error while extracting", e.toString())
            return
        }
        Toast.makeText(context, context.getString(R.string.extracting_successful), Toast.LENGTH_LONG).show()
    }

    private fun createSubDirectories(zipEntryName: String, baseFolderPath: String) {
        var subPath = ""

        for (i in zipEntryName.indices) {
            if (zipEntryName[i] != '/') subPath += zipEntryName[i]
            else {
                File(baseFolderPath + File.separator + subPath).mkdir()
                subPath += '/'
            }
        }
    }
}