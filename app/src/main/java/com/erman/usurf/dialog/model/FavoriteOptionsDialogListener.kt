package com.erman.usurf.dialog.model

import androidx.lifecycle.LiveData
import com.erman.usurf.home.ui.HomeUiState

interface FavoriteOptionsDialogListener {
    fun onRenameButtonClick(path: String, currentName: String)

    fun onRename(path: String, newName: String)

    fun onDelete(path: String)

    fun onDismiss()

    fun getUiState(): LiveData<HomeUiState>
}
