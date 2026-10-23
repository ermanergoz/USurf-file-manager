package com.erman.usurf.directory.ui

import com.erman.usurf.directory.model.FileModel
import java.io.File

// The folder screen's state changes that need nothing but the state: the selection, the panels and copy or move
// mode. The view model applies them; none of them reads a file or emits an event.

/**
 * This state after [toggled] was tapped to select or deselect it. Rename, extract and favorite act on one item, so they
 * are offered only while exactly one is selected.
 */
internal fun DirectoryUiState.select(toggled: FileModel): DirectoryUiState {
    val selection =
        if (toggled.isSelected) {
            common.selectedFiles.filter { it.path != toggled.path }
        } else {
            common.selectedFiles + toggled.copy(isSelected = true)
        }
    val rows = files.map { if (it.path == toggled.path) toggled.copy(isSelected = !toggled.isSelected) else it }
    return copy(common = common.copy(selectedFiles = selection, canRename = selection.size == 1), files = rows)
}

/** This state after a tap on [toggled] while items are selected: deselecting the last one ends the selection. */
internal fun DirectoryUiState.tapWhileSelecting(toggled: FileModel): DirectoryUiState {
    val selected = select(toggled)
    return if (selected.common.selectedFiles.isEmpty()) selected.reset() else selected
}

/** This state with the options panel shown. */
internal fun DirectoryUiState.openPanel(): DirectoryUiState = copy(common = common.copy(isOptionsPanelVisible = true))

/** This state with the options panel, the more menu and the create menu closed, out of copy or move mode. */
internal fun DirectoryUiState.closePanel(): DirectoryUiState = copy(
    common = common.copy(isOptionsPanelVisible = false, isMoreMenuVisible = false, isCreateMenuExpanded = false),
    isInCopyOrMoveMode = false,
    actionType = ActionType.BROWSE
)

/** This state with nothing selected. */
internal fun DirectoryUiState.clearSelection(): DirectoryUiState = copy(
    common = common.copy(selectedFiles = emptyList(), canRename = false),
    files = files.map { it.copy(isSelected = false) }
)

/** This state as an action or the end of copy mode leaves it: the panels closed and nothing selected. */
internal fun DirectoryUiState.reset(): DirectoryUiState = closePanel().clearSelection()

/** This state with the create menu opened, or closed when it was open. */
internal fun DirectoryUiState.toggleCreateMenu(): DirectoryUiState =
    copy(common = common.copy(isCreateMenuExpanded = !common.isCreateMenuExpanded))

/** This state with the more menu opened, or closed when it was open. */
internal fun DirectoryUiState.toggleMoreMenu(): DirectoryUiState = copy(common = common.copy(isMoreMenuVisible = !common.isMoreMenuVisible))

/** This state with the create menu closed, as a create leaves it. */
internal fun DirectoryUiState.closeCreateMenu(): DirectoryUiState = copy(common = common.copy(isCreateMenuExpanded = false))

/**
 * This state in copy or move mode for [actionType], keeping the selection and browsing for a destination. From a
 * search, the destination starts at the folder the search started from, which then has to be listed.
 */
internal fun DirectoryUiState.enterTransfer(actionType: ActionType): DirectoryUiState = copy(
    common = common.copy(isOptionsPanelVisible = false, isMoreMenuVisible = false, isLoading = isSearchMode),
    isInCopyOrMoveMode = true,
    actionType = actionType,
    currentPath = if (isSearchMode) previousPath else currentPath,
    isSearchMode = false,
    query = "",
    files = files.map { it.copy(isSelected = false) }
)

/**
 * This state as the screen shows it again after it was left: outside copy or move mode no panel stays open and
 * nothing stays selected; copy or move mode keeps both.
 */
internal fun DirectoryUiState.resumed(): DirectoryUiState {
    if (isInCopyOrMoveMode) return this
    val hasVisiblePanel = common.isOptionsPanelVisible || common.isMoreMenuVisible || common.isCreateMenuExpanded
    val panelClosed = if (hasVisiblePanel) closePanel() else this
    return if (common.selectedFiles.isNotEmpty()) panelClosed.clearSelection() else panelClosed
}

/** Whether [selection] holds a folder that [destinationPath] is, or lies inside: no folder goes into itself. */
internal fun hasDestinationConflict(selection: List<FileModel>, destinationPath: String): Boolean = selection.any { file ->
    file.isDirectory && (destinationPath == file.path || destinationPath.startsWith(file.path + File.separator))
}
