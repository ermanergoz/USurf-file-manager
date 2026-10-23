package com.erman.usurf.directory.ui

import androidx.annotation.StringRes
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.ROOT_DIRECTORY

data class CommonUi(
    val isLoading: Boolean = false,
    val isOptionsPanelVisible: Boolean = false,
    val isMoreMenuVisible: Boolean = false,
    val isCreateMenuExpanded: Boolean = false,
    val canRename: Boolean = false,
    val selectedFiles: List<FileModel> = emptyList()
)

data class DirectoryUiState(
    val common: CommonUi = CommonUi(),
    val currentPath: String = "",
    val files: List<FileModel> = emptyList(),
    val isInCopyOrMoveMode: Boolean = false,
    val actionType: ActionType = ActionType.BROWSE,
    val activeActionCount: Int = 0,
    val isSearchMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val previousPath: String = "",
    /** The folder above [currentPath] when the app can read it, as the listing of [currentPath] found it; null for a search. */
    val readableParent: String? = null
) {
    val isAtRootDirectory: Boolean
        get() = currentPath == ROOT_DIRECTORY

    val displayPath: String
        get() = if (isSearchMode) query else currentPath

    val currentPathForNavigation: String
        get() = if (isSearchMode) "" else currentPath

    val isLoading: Boolean
        get() = common.isLoading

    val isActionInProgress: Boolean
        get() = activeActionCount > 0

    /** Whether a folder or a search is on screen. */
    val hasActiveLocation: Boolean
        get() = currentPath.isNotEmpty() || isSearchMode

    /** Whether this state shows the folder, or the search, that [other] shows, whatever else differs. */
    fun showsLocationOf(other: DirectoryUiState): Boolean =
        isSearchMode == other.isSearchMode && if (isSearchMode) query == other.query else currentPath == other.currentPath
}

/** What the screen is doing, with the messages an action of this kind shows when it starts, succeeds and fails. */
enum class ActionType(@StringRes val progressMessage: Int, @StringRes val successMessage: Int, @StringRes val failureMessage: Int) {
    COPY(R.string.copying, R.string.copy_successful, R.string.error_while_copying),
    MOVE(R.string.moving, R.string.moving_successful, R.string.error_while_moving),
    RENAME(R.string.renaming, R.string.renaming_successful, R.string.error_while_renaming),
    DELETE(R.string.deleting, R.string.deleting_successful, R.string.error_while_deleting),
    CREATE_FOLDER(R.string.creating, R.string.folder_creation_successful, R.string.error_when_creating_folder),
    CREATE_FILE(R.string.creating, R.string.file_creation_successful, R.string.error_when_creating_file),
    COMPRESS(R.string.compressing, R.string.compressing_successful, R.string.error_while_compressing),
    EXTRACT(R.string.extracting, R.string.extracting_successful, R.string.error_while_extracting),

    // Browsing is never launched as an action, so it has no messages.
    BROWSE(0, 0, 0)
}

/** What a create on the folder screen makes, with the action it runs as. */
enum class CreateKind(val actionType: ActionType) {
    FOLDER(ActionType.CREATE_FOLDER),
    FILE(ActionType.CREATE_FILE)
}

sealed class DirectoryUiEvent {
    data class ShowSnackbar(val messageResId: Int) : DirectoryUiEvent()

    data class ShowDialog(val dialogArgs: DialogArgs) : DirectoryUiEvent()

    /** Ask before deleting the items at these paths. */
    data class ConfirmDelete(val paths: List<String>) : DirectoryUiEvent()

    /** Ask the user to grant the SD card's document tree, so the write that failed can run again. */
    data object RequestSafGrant : DirectoryUiEvent()
}

/** This file as its Information shows it: an entry listed through root has no size or date read, and says so. */
fun FileModel.forInformation(notAvailable: String): FileModel =
    if (isInRoot) copy(size = notAvailable, lastModified = notAvailable) else this
