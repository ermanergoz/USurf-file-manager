package com.erman.usurf.directory.model

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.R
import com.erman.usurf.utils.SHARED_PREF_FILE
import java.io.*
import java.io.File.separator
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

//fun getFiles(path: String,
//             showHidden: Boolean,
//             showFilesOnly: Boolean,
//             showFoldersOnly: Boolean,
//             fileSortMode: String?,
//             isAscending: Boolean,
//             isDescending: Boolean,
//             showFilesOnTop: Boolean,
//             showFoldersOnTop: Boolean): List<File> {
//
//    var files = File(path).listFiles()!!.filter { !it.isHidden || showHidden }.filter { it.isFile || !showFilesOnly }
//        .filter { it.isDirectory || !showFoldersOnly }.toMutableList()
//
//    if (fileSortMode == "Sort by name") {
//        if (isAscending) files =
//            files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop }, { !it.isFile || !showFilesOnTop }, { it.name })).toMutableList()
//        else if (isDescending) {
//            files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
//                                                     { !it.isFile || !showFilesOnTop }).thenByDescending { it.name }).toMutableList()
//        }
//    }
//
//    if (fileSortMode == "Sort by size") {
//        if (isAscending) files = files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop },
//                                                            { !it.isFile || !showFilesOnTop },
//                                                            { it.length() },
//                                                            {
//                                                                getFolderSize(
//                                                                    it.path
//                                                                )
//                                                            })).toMutableList()
//        else if (isDescending) {
//            files = files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop },
//                                               { !it.isFile || !showFilesOnTop },
//                                               { it.length() },
//                                               {
//                                                   getFolderSize(
//                                                       it.path
//                                                   )
//                                               })).asReversed().toMutableList()
//            /*asReversed() is a view of the sorted list with reversed index and has better performance than using reverse()*/
//            /*in this case we have 2 descending fields and compareByDescending can take only one*/
//        }
//    }
//    if (fileSortMode == "Sort by last modified") {
//        if (isAscending) files =
//            files.sortedWith(compareBy({ !it.isDirectory || !showFoldersOnTop }, { !it.isFile || !showFilesOnTop }, { it.lastModified() }))
//                .toMutableList()
//        else if (isDescending) {
//            files = files.sortedWith(compareBy<File>({ !it.isDirectory || !showFoldersOnTop },
//                                                     { !it.isFile || !showFilesOnTop }).thenByDescending { it.lastModified() }).toMutableList()
//        }
//    }
//    return files
//}
