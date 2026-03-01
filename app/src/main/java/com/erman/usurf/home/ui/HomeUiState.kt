package com.erman.usurf.home.ui

import com.erman.usurf.dialog.model.DialogArgs

data class HomeUiState(
    val path: String = "",
    val isRenameMode: Boolean = false,
)

sealed class HomeUiEvent {
    data class NavigateToDirectory(val actionId: Int, val path: String) : HomeUiEvent()
    data class ShowDialog(val dialogArgs: DialogArgs) : HomeUiEvent()
    data class ShowToast(val messageResId: Int) : HomeUiEvent()
}
