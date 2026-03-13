package com.erman.usurf.directory.ui

import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.ROOT_DIRECTORY

data class CommonUi(
    val isLoading: Boolean = false,
    val isOptionsPanelVisible: Boolean = false,
    val isMoreMenuVisible: Boolean = false,
    val isCreateMenuExpanded: Boolean = false,
    val canRename: Boolean = false,
    val selectedFiles: List<FileModel> = emptyList(),
)

data class DirectoryUiState(
    val common: CommonUi = CommonUi(),
    val currentPath: String = "",
    val files: List<FileModel> = emptyList(),
    val isInCopyOrMoveMode: Boolean = false,
    val actionType: ActionType = ActionType.BROWSE,
    val activeActionCount: Int = 0,
    val isSearchMode: Boolean = false,
    val query: String = "",
    val previousPath: String = "",
) {
    val isAtRootDirectory: Boolean
        get() = currentPath == ROOT_DIRECTORY

    val displayPath: String
        get() = if (isSearchMode) query else currentPath

    val currentPathForNavigation: String
        get() = if (isSearchMode) "" else currentPath

    val fileList: List<FileModel>
        get() = files

    val isLoading: Boolean
        get() = common.isLoading

    val isActionInProgress: Boolean
        get() = activeActionCount > 0
}

enum class ActionType {
    COPY,
    MOVE,
    RENAME,
    DELETE,
    CREATE_FOLDER,
    CREATE_FILE,
    COMPRESS,
    EXTRACT,
    BROWSE,
}

sealed class DirectoryUiEvent {
    data class ShowSnackbar(val messageResId: Int) : DirectoryUiEvent()

    data class ShowDialog(val dialogArgs: DialogArgs) : DirectoryUiEvent()
}
