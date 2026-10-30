package com.erman.usurf.directory.ui

import androidx.annotation.StringRes
import com.erman.usurf.R
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

    /** The path bar shows a folder's path; a search shows its query instead. */
    val isPathVisible: Boolean
        get() = !isSearchMode

    /** The bar that pastes or cancels shows while a copy or a move looks for its destination. */
    val isConfirmationBarVisible: Boolean
        get() = isInCopyOrMoveMode

    /** The actions on the selection show while items are selected, outside a copy or a move. */
    val isOptionBarVisible: Boolean
        get() = common.isOptionsPanelVisible && !isInCopyOrMoveMode

    /** The actions on one item only (favourite, extract, rename) are enabled while one item is selected. */
    val actionButtonsEnabled: Boolean
        get() = common.canRename

    /** A listed folder with nothing in it says so. */
    val isEmptyFolderVisible: Boolean
        get() = !isLoading && files.isEmpty()

    /** The create button shows in a listed folder while no item is selected. */
    val isCreateFabVisible: Boolean
        get() = !isSearchMode && !common.isOptionsPanelVisible && !isLoading

    /** The create menu's two buttons show while it is open and the create button could be used. */
    val isCreateMenuVisible: Boolean
        get() = common.isCreateMenuExpanded && !common.isOptionsPanelVisible && !isLoading

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

/**
 * What the folder screen is asked to do once. A dialog or navigation event is emitted only synchronously, from the user
 * intent that asks for it; an operation that completes later emits [ShowSnackbar] or a new state, never a dialog, so
 * no dialog opens on a screen the user has left or over one the user opened since. The one exception is
 * [RequestSafGrant], the permission request: a write the SD card refuses asks for the grant it needs and is retried
 * once after it, so the request continues the user's own intent.
 */
sealed class DirectoryUiEvent {
    /** Show the message [messageResId], its one placeholder, when it has one, filled with [formatArg]. */
    data class ShowSnackbar(@StringRes val messageResId: Int, val formatArg: String? = null) : DirectoryUiEvent()

    /** Ask for a new name for the item at [path], now named [name]. */
    data class ShowRenameDialog(val path: String, val name: String, val isDirectory: Boolean) : DirectoryUiEvent()

    /** Show the information of [files], one page each. */
    data class ShowInformation(val files: List<FileModel>) : DirectoryUiEvent()

    data object ShowCreateFolderDialog : DirectoryUiEvent()

    data object ShowCreateFileDialog : DirectoryUiEvent()

    /** Ask for the name of the archive the items held for the dialog are compressed into. */
    data object ShowCompressDialog : DirectoryUiEvent()

    data object ShowSearchDialog : DirectoryUiEvent()

    /** Ask for the name the folder at [path] is added to the favourites with, offering [defaultName]; none offers the screen's own. */
    data class ShowAddFavoriteDialog(val path: String, val defaultName: String?) : DirectoryUiEvent()

    data class OpenFile(val path: String) : DirectoryUiEvent()

    /** Share the files at [paths]; the folders of the selection, named by [directoryNames], cannot be shared. */
    data class Share(val paths: List<String>, val directoryNames: List<String>) : DirectoryUiEvent()

    /** Ask before deleting the items at these paths. */
    data class ConfirmDelete(val paths: List<String>) : DirectoryUiEvent()

    /** Ask the user to grant the SD card's document tree, so the write that failed can run again. */
    data object RequestSafGrant : DirectoryUiEvent()
}
