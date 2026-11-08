package com.erman.usurf.directory.ui

import com.erman.usurf.directory.model.FileModel

interface FileItemListener {
    fun onFileClick(file: FileModel)

    fun onFileLongClick(file: FileModel): Boolean
}
