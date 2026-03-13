package com.erman.usurf.dialog.model

import android.widget.TextView
import com.erman.usurf.directory.model.FileModel

sealed class DialogArgs {
    data class RenameDialogArgs(val name: String?) : DialogArgs()
    data class InformationDialogArgs(val file: FileModel) : DialogArgs()
    object CreateFolderDialogArgs : DialogArgs()
    object CreateFileDialogArgs : DialogArgs()
    object CompressDialogArgs : DialogArgs()
    data class OpenFileActivityArgs(val path: String) : DialogArgs()
    data class ShareActivityArgs(val multipleSelectionList: List<FileModel>) : DialogArgs()
    data class AddFavoriteDialogArgs(val path: String) : DialogArgs()
    data class FavoriteOptionsDialogArgs(val view: TextView) : DialogArgs()
    object FileSearchDialogArgs : DialogArgs()
    data class KitkatRemovableStorageDialogArgs(val isKitkatRemovableStorage: Boolean) : DialogArgs()
    object SAFActivityArgs : DialogArgs()
}