package com.erman.usurf.directory.ui

import com.erman.usurf.utils.ROOT_DIRECTORY
import java.io.File

/** What Back does on the folder screen. */
sealed class BackAction {
    /** Close the options panel or the create menu and drop the selection. */
    data object ClosePanel : BackAction()

    /** Leave the search for the folder at [path]. */
    data class LeaveSearch(val path: String) : BackAction()

    /** Open the folder above, at [path]. */
    data class GoUp(val path: String) : BackAction()

    /** Handle Back and change nothing, as copy or move mode does where it cannot go further up. */
    data object Stay : BackAction()

    /** Leave the screen. */
    data object Leave : BackAction()
}

/**
 * Decides Back from the screen's state alone, in this order: copy or move mode goes up while it can and otherwise
 * stays; an open panel closes; a search returns to the folder it started from; a folder goes up while it can and
 * otherwise leaves the screen. Going up needs the folder above to be readable and not the top of the file system,
 * as the listing found it, or root. While a folder is still being opened its parent is not known yet, so Back stays
 * rather than leave a screen the next moment would have gone up from.
 */
internal object BackNavigationPolicy {
    fun decide(state: DirectoryUiState, rootEnabled: Boolean): BackAction {
        val common = state.common
        return when {
            state.isInCopyOrMoveMode -> wayUp(state, rootEnabled) ?: BackAction.Stay
            common.isOptionsPanelVisible || common.isCreateMenuExpanded -> BackAction.ClosePanel
            state.isSearchMode -> BackAction.LeaveSearch(state.previousPath.ifEmpty { ROOT_DIRECTORY })
            else -> wayUp(state, rootEnabled) ?: if (isParentStillBeingRead(state)) BackAction.Stay else BackAction.Leave
        }
    }

    // Only a folder that has one: the top of the file system has no parent to wait for.
    private fun isParentStillBeingRead(state: DirectoryUiState): Boolean =
        state.readableParent == null && state.common.isLoading && File(state.currentPathForNavigation).parent != null

    private fun wayUp(state: DirectoryUiState, rootEnabled: Boolean): BackAction.GoUp? {
        val parent = File(state.currentPathForNavigation).parent ?: return null
        // Only a parent the listing of this very folder found readable counts; one read for another folder does not.
        val canReadParent = state.readableParent == parent && parent != ROOT_DIRECTORY
        return if (canReadParent || rootEnabled) BackAction.GoUp(parent) else null
    }
}
