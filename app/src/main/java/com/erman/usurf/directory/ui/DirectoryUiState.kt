package com.erman.usurf.directory.ui

import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.Event

data class CommonUi(
    val isLoading: Boolean = false,
    val toastMessage: Event<Int>? = null,
    val dialogEvent: Event<DialogArgs>? = null,
    val isOptionsPanelVisible: Boolean = false,
    val isMoreMenuVisible: Boolean = false,
    val isCreateMenuExpanded: Boolean = false,
    val canRename: Boolean = false,
    val selectedFiles: List<FileModel> = emptyList(),
    val isInRoot: Boolean = false,
)

sealed class DirectoryUIState {
    abstract val common: CommonUi
    abstract val displayPath: String
    abstract val currentPathForNavigation: String
    abstract val fileList: List<FileModel>
    abstract val isLoading: Boolean
    abstract val isInCopyOrMoveMode: Boolean
    abstract val isSearchMode: Boolean

    data class Browsing(
        override val common: CommonUi = CommonUi(),
        val currentPath: String = "",
        val files: List<FileModel> = emptyList(),
    ) : DirectoryUIState() {
        override val displayPath: String get() = currentPath
        override val currentPathForNavigation: String get() = currentPath
        override val fileList: List<FileModel> get() = files
        override val isLoading: Boolean get() = common.isLoading
        override val isInCopyOrMoveMode: Boolean get() = false
        override val isSearchMode: Boolean get() = false
    }

    data class Searching(
        override val common: CommonUi = CommonUi(),
        val query: String = "",
        val results: List<FileModel> = emptyList(),
        val previousPath: String = "",
    ) : DirectoryUIState() {
        override val displayPath: String get() = query
        override val currentPathForNavigation: String get() = ""
        override val fileList: List<FileModel> get() = results
        override val isLoading: Boolean get() = common.isLoading
        override val isInCopyOrMoveMode: Boolean get() = false
        override val isSearchMode: Boolean get() = true
    }

    sealed class ExecutingOperation : DirectoryUIState() {
        data class FileAction(
            override val common: CommonUi = CommonUi(),
            val actionType: ActionType,
            val currentPath: String = "",
            val files: List<FileModel> = emptyList(),
        ) : ExecutingOperation() {
            override val displayPath: String get() = currentPath
            override val currentPathForNavigation: String get() = currentPath
            override val fileList: List<FileModel> get() = files
            override val isLoading: Boolean get() = common.isLoading
            override val isInCopyOrMoveMode: Boolean get() = true
            override val isSearchMode: Boolean get() = false
        }
    }
}

enum class ActionType { COPY, MOVE }

sealed class DirectoryUiEvent {
    data class ShowToast(val messageResId: Int) : DirectoryUiEvent()
    data class ShowDialog(val dialogArgs: DialogArgs) : DirectoryUiEvent()
}
