package com.erman.usurf.home.ui

import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.home.model.StorageItem

data class HomeUiState(
    val path: String = "",
    val isRenameMode: Boolean = false,
    val storageItems: List<StorageItem> = emptyList(),
)

sealed class HomeUiEvent {
    data class NavigateToDirectory(val path: String) : HomeUiEvent()

    data class ShowDialog(val dialogArgs: DialogArgs) : HomeUiEvent()

    data class ShowSnackbar(val messageResId: Int) : HomeUiEvent()
}
