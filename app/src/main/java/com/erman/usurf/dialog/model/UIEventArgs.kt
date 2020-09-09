package com.erman.usurf.dialog.model

import com.erman.usurf.directory.model.FileModel

sealed class UIEventArgs {
    data class RenameDialogArgs(val name: String?)
    data class InformationDialogArgs(val file: FileModel) : UIEventArgs()
    object CreateFolderDialogArgs : UIEventArgs()
    object CreateFileDialogArgs : UIEventArgs()
    data class OpenFileActivityArgs(val path: String)
    data class ShareActivityArgs(val multipleSelectionList: List<FileModel>)
    object SAFActivityArgs : UIEventArgs()
}