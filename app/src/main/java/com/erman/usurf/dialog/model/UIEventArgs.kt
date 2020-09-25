package com.erman.usurf.dialog.model

import android.widget.TextView
import com.erman.usurf.directory.model.FileModel

sealed class UIEventArgs {
    data class RenameDialogArgs(val name: String?)
    data class InformationDialogArgs(val file: FileModel) : UIEventArgs()
    object CreateFolderDialogArgs : UIEventArgs()
    object CreateFileDialogArgs : UIEventArgs()
    object CompressDialogArgs : UIEventArgs()
    data class OpenFileActivityArgs(val path: String)
    data class ShareActivityArgs(val multipleSelectionList: List<FileModel>)
    object SAFActivityArgs : UIEventArgs()
    data class ShortcutDialogArgs(val path: String)
    data class ShortcutOptionsDialogArgs(val view: TextView) : UIEventArgs()
}