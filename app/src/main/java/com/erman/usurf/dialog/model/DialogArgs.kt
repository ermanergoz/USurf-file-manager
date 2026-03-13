package com.erman.usurf.dialog.model

import com.erman.usurf.directory.model.FileModel

sealed class DialogArgs {
    data class RenameDialogArgs(val name: String?) : DialogArgs()

    data class InformationDialogArgs(val file: FileModel) : DialogArgs()

    data object CreateFolderDialogArgs : DialogArgs()

    data object CreateFileDialogArgs : DialogArgs()

    data object CompressDialogArgs : DialogArgs()

    data class OpenFileActivityArgs(val path: String) : DialogArgs()

    data class ShareActivityArgs(val multipleSelectionList: List<FileModel>) : DialogArgs()

    data class AddFavoriteDialogArgs(val path: String) : DialogArgs()

    data class FavoriteOptionsDialogArgs(val favoritePath: String, val favoriteName: String) : DialogArgs()

    data object FileSearchDialogArgs : DialogArgs()

    data class KitkatRemovableStorageDialogArgs(val isKitkatRemovableStorage: Boolean) : DialogArgs()

    data object SAFActivityArgs : DialogArgs()
}
