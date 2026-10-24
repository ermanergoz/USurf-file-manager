package com.erman.usurf.dialog.model

import com.erman.usurf.directory.model.FileModel

sealed class DialogArgs {
    data class RenameDialogArgs(val path: String, val name: String, val isDirectory: Boolean) : DialogArgs()

    data class InformationDialogArgs(val file: FileModel) : DialogArgs()

    data object CreateFolderDialogArgs : DialogArgs()

    data object CreateFileDialogArgs : DialogArgs()

    data object CompressDialogArgs : DialogArgs()

    data class OpenFileActivityArgs(val path: String) : DialogArgs()

    data class ShareActivityArgs(val shareablePaths: List<String>, val directoryNames: List<String>) : DialogArgs()

    data class AddFavoriteDialogArgs(val path: String) : DialogArgs()

    data class FavoriteOptionsDialogArgs(val favoritePath: String, val favoriteName: String) : DialogArgs()

    data object FileSearchDialogArgs : DialogArgs()

    data object SAFActivityArgs : DialogArgs()
}
