package com.erman.usurf.dialog.model

import android.widget.TextView
import com.erman.usurf.directory.model.FileModel

sealed class DialogArgs {
    data class RenameDialogArgs(val name: String?)
    data class InformationDialogArgs(val file: FileModel) : DialogArgs()
    object CreateFolderDialogArgs : DialogArgs()
    object CreateFileDialogArgs : DialogArgs()
    object CompressDialogArgs : DialogArgs()
    data class OpenFileActivityArgs(val path: String)
    data class ShareActivityArgs(val multipleSelectionList: List<FileModel>)
    data class FavoriteDialogArgs(val path: String)
    data class FavoriteOptionsDialogArgs(val view: TextView) : DialogArgs()
    object FileSearchDialogArgs : DialogArgs()
}