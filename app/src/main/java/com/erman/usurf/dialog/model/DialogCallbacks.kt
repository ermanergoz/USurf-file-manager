package com.erman.usurf.dialog.model

import androidx.lifecycle.LiveData
import com.erman.usurf.home.ui.HomeUiState

fun interface OnSearchOkPressedListener {
    fun onSearchOkPressed(query: String)
}

fun interface OnFolderCreateOkPressedListener {
    fun onFolderCreateOkPressed(name: String)
}

fun interface OnFileCreateOkPressedListener {
    fun onFileCreateOkPressed(name: String)
}

fun interface OnCompressOkPressedListener {
    fun onCompressOkPressed(name: String)
}

fun interface OnRenameOkPressedListener {
    fun onRenameOkPressed(name: String)
}

interface AddFavoriteDialogCallbacks {
    fun onDialogShown()

    fun onAddFavorite(
        path: String,
        name: String,
    )
}

interface FavoriteOptionsDialogListener {
    fun onRenameButtonClick(
        path: String,
        currentName: String,
    )

    fun onRename(
        path: String,
        newName: String,
    )

    fun onDelete(path: String)

    fun onDismiss()

    fun getUiState(): LiveData<HomeUiState>
}

fun interface ManageAllFilesRequestCallbacks {
    fun onManageAllFilesRequested()
}

fun interface SafAccessRequestCallbacks {
    fun onSafAccessRequested()
}
