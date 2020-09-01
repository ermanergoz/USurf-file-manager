package com.erman.usurf.directory.model

import java.io.File
import java.lang.Exception

class DirectoryModel() {

    fun getFiles(
        path: String/*,
        showHidden: Boolean,
        showFilesOnly: Boolean,
        showFoldersOnly: Boolean,
        fileSortMode: String?,
        isAscending: Boolean,
        isDescending: Boolean,
        showFilesOnTop: Boolean,
        showFoldersOnTop: Boolean*/
    ): List<File> {
        var files = listOf<File>()
        try {
            files = File(path).listFiles()!!.toList()
        } catch (err: Exception) {
            err.printStackTrace()
        }


        //var files = File(path).listFiles()!!.filter { !it.isHidden || showHidden }
        //    .filter { it.isFile || !showFilesOnly }
        //    .filter { it.isDirectory || !showFoldersOnly }.toMutableList()
//
        //if (fileSortMode == "Sort by name") {
        //    if (isAscending) files =
        //        files.sortedWith(
        //            compareBy(
        //                { !it.isDirectory || !showFoldersOnTop },
        //                { !it.isFile || !showFilesOnTop },
        //                { it.name })
        //        ).toMutableList()
        //    else if (isDescending) {
        //        files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
        //            { !it.isFile || !showFilesOnTop }).thenByDescending { it.name }).toMutableList()
        //    }
        //}
//
        //if (fileSortMode == "Sort by size") {
        //    if (isAscending) files = files.sortedWith(
        //        compareBy({ !it.isDirectory || !showFoldersOnTop },
        //            { !it.isFile || !showFilesOnTop },
        //            { it.length() },
        //            {
        //                getFolderSize(
        //                    it.path
        //                )
        //            })
        //    ).toMutableList()
        //    else if (isDescending) {
        //        files = files.sortedWith(
        //            compareBy({ !it.isDirectory || !showFoldersOnTop },
        //                { !it.isFile || !showFilesOnTop },
        //                { it.length() },
        //                {
        //                    getFolderSize(
        //                        it.path
        //                    )
        //                })
        //        ).asReversed().toMutableList()
        //        /*asReversed() is a view of the sorted list with reversed index and has better performance than using reverse()*/
        //        /*in this case we have 2 descending fields and compareByDescending can take only one*/
        //    }
        //}
        //if (fileSortMode == "Sort by last modified") {
        //    if (isAscending) files =
        //        files.sortedWith(
        //            compareBy(
        //                { !it.isDirectory || !showFoldersOnTop },
        //                { !it.isFile || !showFilesOnTop },
        //                { it.lastModified() })
        //        )
        //            .toMutableList()
        //    else if (isDescending) {
        //        files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
        //            { !it.isFile || !showFilesOnTop }).thenByDescending { it.lastModified() })
        //            .toMutableList()
        //    }
        //}
        return files
    }

    fun getFolderSize(path: String): Double {
        if (File(path).exists()) {
            var size = 0.0

            var fileList = File(path).listFiles().toList()

            for (i in fileList.indices) {
                if (fileList[i].isDirectory) {
                    size += getFolderSize(fileList[i].path)
                } else size += fileList[i].length()
            }
            return size
        }
        return 0.0
    }

    fun getConvertedFileSize(size: Long): String {
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
            else -> size.toDouble().toString() + " Bytes"
        }
        return sizeStr
    }

    fun getConvertedFileSize(size: Double): String {
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
            else -> size.toDouble().toString() + " Bytes"
        }
        return sizeStr
    }
}