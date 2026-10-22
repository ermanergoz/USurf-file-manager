package com.erman.usurf.directory.ui

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.ArchiveOperations
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.directory.model.FileOperationException
import com.erman.usurf.directory.model.OperationError
import com.erman.usurf.directory.model.operationMessageOr
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DirectoryViewModel(
    private val directoryModel: DirectoryModel,
    private val preferencesRepository: PreferencesRepository,
    private val storageAccessPolicy: StorageAccessPolicy,
    private val archiveOperations: ArchiveOperations
) : ViewModel() {
    private val isMultiSelectionMode: Boolean
        get() = with(requireCurrentState()) { !isInCopyOrMoveMode && common.selectedFiles.isNotEmpty() }

    private val _uiState = MutableLiveData(DirectoryUiState())
    val uiState: LiveData<DirectoryUiState> = _uiState

    private val _uiEvents = MutableLiveData<Event<DirectoryUiEvent>>()
    val uiEvents: LiveData<Event<DirectoryUiEvent>> = _uiEvents

    private var listLoadJob: Job? = null

    private var actionAwaitingGrant: (() -> Unit)? = null

    val shouldShowThumbnails: Boolean
        get() = preferencesRepository.getShowThumbnailsPreference()

    private fun updateState(transform: (DirectoryUiState) -> DirectoryUiState) {
        val current = requireCurrentState()
        val newState = transform(current)
        if (newState != current) {
            _uiState.value = newState
        }
    }

    private fun requireCurrentState(): DirectoryUiState = _uiState.value ?: DirectoryUiState()

    private fun updateCommon(transform: (CommonUi) -> CommonUi) {
        updateState { state -> state.copy(common = transform(state.common)) }
    }

    // Every failure of the directory model reaches here as a FileOperationException, the one type caught on this
    // screen; anything else is a programming error and is left to surface.
    private fun launchAction(
        actionType: ActionType,
        progressMessageResId: Int,
        successMessageResId: Int,
        errorMessageResId: Int,
        finallyStateTransform: (DirectoryUiState) -> DirectoryUiState = { it },
        block: suspend () -> Unit
    ) {
        actionAwaitingGrant = null
        updateState { it.copy(actionType = actionType, activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(progressMessageResId))
        viewModelScope.launch {
            try {
                block()
                reload()
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(successMessageResId))
            } catch (err: FileOperationException) {
                reportFailure(err, errorMessageResId) {
                    launchAction(actionType, progressMessageResId, successMessageResId, errorMessageResId, finallyStateTransform, block)
                }
            } finally {
                updateState { state ->
                    val base =
                        state.copy(
                            actionType = ActionType.BROWSE,
                            activeActionCount = state.activeActionCount - 1
                        )
                    finallyStateTransform(base)
                }
            }
        }
    }

    private fun reportFailure(err: FileOperationException, @StringRes errorMessageResId: Int, retry: () -> Unit) {
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(err.operationMessageOr(errorMessageResId)))
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        if (err.error == OperationError.SafGrantRequired) {
            actionAwaitingGrant = retry
            _uiEvents.value = Event(DirectoryUiEvent.RequestSafGrant)
        }
    }

    // On API 29 and lower the SD card refuses file API writes until its document tree is granted, whatever
    // canWrite() says, and the refusal reaches here only as an operation that failed. When the access policy
    // says a grant would help, such a failure asks for the grant; an operation error of its own keeps its message,
    // and every check the model makes before it writes (a taken or invalid name) fails with an error of its own.
    private suspend fun writingTo(paths: List<String>, write: suspend () -> Unit) {
        try {
            write()
        } catch (err: FileOperationException) {
            throw grantFailureOr(err, paths)
        }
    }

    private fun grantFailureOr(err: FileOperationException, paths: List<String>): FileOperationException =
        if (err.error == null && paths.any { storageAccessPolicy.needsSafGrant(it) }) {
            FileOperationException("SD card access is needed", OperationError.SafGrantRequired, err)
        } else {
            err
        }

    /** Runs again the write that asked for the SD card grant, once, now that the user granted it. */
    fun onSafAccessGranted() {
        val retry = actionAwaitingGrant ?: return
        actionAwaitingGrant = null
        retry()
    }

    // Rename, extract and favorite act on one item, so they are offered only while exactly one is selected.
    private fun applySelectionChange(state: DirectoryUiState, newSelection: List<FileModel>, toggledFile: FileModel): DirectoryUiState {
        val updatedCommon = state.common.copy(selectedFiles = newSelection, canRename = newSelection.size == 1)
        val updatedFileList =
            state.fileList.map {
                if (it.path == toggledFile.path) {
                    toggledFile.copy(isSelected = !toggledFile.isSelected)
                } else {
                    it
                }
            }
        return state.copy(common = updatedCommon, files = updatedFileList)
    }

    fun onFileClick(file: FileModel) {
        val state = requireCurrentState()
        val common = state.common
        if (isMultiSelectionMode) {
            val newSelection =
                directoryModel.manageMultipleSelectionList(file, common.selectedFiles)
            if (newSelection.isEmpty()) {
                turnOffOptionPanel()
                clearMultipleSelection()
            } else {
                updateState { uiState -> applySelectionChange(uiState, newSelection, file) }
            }
        } else {
            if (file.isDirectory) {
                setPath(file.path)
            } else {
                _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.OpenFileActivityArgs(file.path)))
            }
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        val state = requireCurrentState()
        if (state.isInCopyOrMoveMode) return false
        val newSelection =
            directoryModel.manageMultipleSelectionList(file, state.common.selectedFiles)
        updateState { uiState ->
            val updated = applySelectionChange(uiState, newSelection, file)
            updated.copy(
                common =
                updated.common.copy(
                    isOptionsPanelVisible = true
                )
            )
        }
        return true
    }

    fun turnOffOptionPanel() {
        updateState { state ->
            val cleared =
                state.common.copy(
                    isOptionsPanelVisible = false,
                    isMoreMenuVisible = false,
                    isCreateMenuExpanded = false
                )
            state.copy(
                common = cleared,
                isInCopyOrMoveMode = false,
                actionType = ActionType.BROWSE
            )
        }
    }

    fun clearMultipleSelection() {
        val clearedFiles = requireCurrentState().fileList.map { it.copy(isSelected = false) }
        updateState { uiState ->
            uiState.copy(
                common = uiState.common.copy(selectedFiles = emptyList(), canRename = false),
                files = clearedFiles
            )
        }
    }

    fun endCopyMode() {
        turnOffOptionPanel()
        clearMultipleSelection()
    }

    private fun navigateToParent(): Boolean {
        val state = requireCurrentState()
        val currentPath = state.currentPathForNavigation
        val parentPath = File(currentPath).parent ?: return false
        val prevFile = File(parentPath)
        val canNavigate =
            (prevFile.canRead() && prevFile.path != ROOT_DIRECTORY) ||
                preferencesRepository.getRootAccessPreference()
        if (!canNavigate) return false
        setPath(prevFile.path)
        return true
    }

    private fun handleCopyOrMoveBack(): Boolean {
        val state = requireCurrentState()
        if (!state.isInCopyOrMoveMode) return false
        val currentPath = state.currentPathForNavigation
        val parentPath = File(currentPath).parent ?: return true
        val prevFile = File(parentPath)
        val canNavigate =
            (prevFile.canRead() && prevFile.path != ROOT_DIRECTORY) ||
                preferencesRepository.getRootAccessPreference()
        if (canNavigate) setPath(prevFile.path)
        return true
    }

    private fun handleOptionsPanelBack(): Boolean {
        val state = requireCurrentState()
        val inOptionOrMenu = state.common.isOptionsPanelVisible || state.common.isCreateMenuExpanded
        if (!inOptionOrMenu) return false
        turnOffOptionPanel()
        clearMultipleSelection()
        return true
    }

    private fun handleSearchModeBack(): Boolean {
        val state = requireCurrentState()
        if (!state.isSearchMode) return false
        setPath(state.previousPath.ifEmpty { ROOT_DIRECTORY })
        return true
    }

    fun onBackPressed(): Boolean = handleCopyOrMoveBack() || handleOptionsPanelBack() || handleSearchModeBack() || navigateToParent()

    fun shouldConfirmExit(): Boolean = requireCurrentState().isActionInProgress

    fun hasActiveLocation(): Boolean = with(requireCurrentState()) { currentPath.isNotEmpty() || isSearchMode }

    private fun requireNonEmptySelectionAndResetPanel(): List<FileModel>? {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return null
        val selectionCopy = selection.toList()
        turnOffOptionPanel()
        clearMultipleSelection()
        return selectionCopy
    }

    fun setPath(path: String) {
        actionAwaitingGrant = null
        updateState {
            it.copy(
                common = it.common.copy(isLoading = true),
                currentPath = path,
                files = emptyList(),
                isSearchMode = false,
                query = ""
            )
        }
        reload()
    }

    // Every listing of the screen, on entry, after an operation or on a swipe, runs here and only one at a time: a new
    // one cancels the one in flight, and a result is applied only while the screen still shows the folder or the
    // search it was read for, so a late answer never replaces the rows of the folder the user went to since.
    private fun reload(state: DirectoryUiState = requireCurrentState()) {
        listLoadJob?.cancel()
        // Without a folder or a search on screen there is nothing to list.
        if (!state.isSearchMode && state.currentPath.isEmpty()) {
            updateState { it.copy(common = it.common.copy(isLoading = false), isRefreshing = false) }
            return
        }
        listLoadJob = viewModelScope.launch {
            try {
                val files =
                    if (state.isSearchMode) {
                        directoryModel.getSearchedDeviceFiles(state.query)
                    } else {
                        directoryModel.getFileModelsFromDirectory(state.currentPath)
                    }
                if (!requireCurrentState().showsLocationOf(state)) return@launch
                // A search the user waited for says when it found nothing; a refresh of one leaves the message it follows.
                if (state.isSearchMode && state.isLoading && files.isEmpty()) {
                    _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.empty_folder))
                }
                updateState { current ->
                    current.copy(
                        common = current.common.copy(
                            isLoading = false,
                            isCreateMenuExpanded = current.common.isCreateMenuExpanded && !state.isSearchMode
                        ),
                        files = files,
                        isRefreshing = false
                    )
                }
            } catch (err: FileOperationException) {
                if (!requireCurrentState().showsLocationOf(state)) return@launch
                updateState { it.copy(common = it.common.copy(isLoading = false), isRefreshing = false) }
                val fallbackResId = if (state.isSearchMode) R.string.search_failed else R.string.unable_to_open_directory
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(err.operationMessageOr(fallbackResId)))
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            }
        }
    }

    fun compress() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.CompressDialogArgs))
    }

    fun onSwipeRefresh() {
        updateState { it.copy(isRefreshing = true) }
        reload()
    }

    fun onFileCompressOkPressed(zipNameWithExtension: String) {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(
            ActionType.COMPRESS,
            R.string.compressing,
            R.string.compressing_successful,
            R.string.error_while_compressing
        ) {
            // The archive is written into the folder of the selection.
            val archiveFolder = File(selection.first().path).parent ?: ROOT_DIRECTORY
            writingTo(listOf(archiveFolder)) { archiveOperations.compressFiles(selection, zipNameWithExtension) }
        }
    }

    fun extract() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(
            ActionType.EXTRACT,
            R.string.extracting,
            R.string.extracting_successful,
            R.string.error_while_extracting
        ) {
            writingTo(listOf(selection.first().path)) { archiveOperations.extractFiles(selection.first()) }
        }
    }

    fun showFileInformation() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        selection.forEach { file ->
            _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.InformationDialogArgs(file)))
        }
    }

    /** The size of the folder at [path], read when its Information asks for it; empty when it cannot be read. */
    fun folderSizeOf(path: String): LiveData<String> {
        val size = MutableLiveData<String>()
        viewModelScope.launch {
            size.value =
                try {
                    directoryModel.getFolderSize(path)
                } catch (err: FileOperationException) {
                    loge(err.localizedMessage ?: UNKNOWN_ERROR)
                    ""
                }
        }
        return size
    }

    fun share() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val shareablePaths = selection.filterNot { it.isDirectory }.map { it.path }
        val directoryNames = selection.filter { it.isDirectory }.map { it.name }
        _uiEvents.value =
            Event(DirectoryUiEvent.ShowDialog(DialogArgs.ShareActivityArgs(shareablePaths, directoryNames)))
    }

    fun showMoreOption() {
        val state = requireCurrentState()
        if (state.common.isMoreMenuVisible) {
            updateCommon { it.copy(isMoreMenuVisible = false) }
        } else {
            updateCommon { it.copy(isMoreMenuVisible = true) }
        }
    }

    fun copy() = enterTransferMode(ActionType.COPY)

    fun move() = enterTransferMode(ActionType.MOVE)

    private fun enterTransferMode(actionType: ActionType) {
        val state = requireCurrentState()
        if (state.isInCopyOrMoveMode) return
        val targetPath = if (state.isSearchMode) state.previousPath else state.currentPath
        val clearedFiles = state.fileList.map { it.copy(isSelected = false) }
        updateState { uiState ->
            uiState.copy(
                common =
                uiState.common.copy(
                    isOptionsPanelVisible = false,
                    isMoreMenuVisible = false,
                    isLoading = state.isSearchMode
                ),
                isInCopyOrMoveMode = true,
                actionType = actionType,
                currentPath = targetPath,
                isSearchMode = false,
                query = "",
                files = clearedFiles
            )
        }
        if (state.isSearchMode) reload()
    }

    private fun transferErrorMessage(actionType: ActionType): Int =
        if (actionType == ActionType.MOVE) R.string.error_while_moving else R.string.error_while_copying

    private fun launchTransfer(actionType: ActionType, selection: List<FileModel>, path: String) {
        val errorMessage = transferErrorMessage(actionType)
        if (selection.isEmpty()) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(errorMessage))
            return
        }
        val isMove = actionType == ActionType.MOVE
        actionAwaitingGrant = null
        updateState { it.copy(activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(if (isMove) R.string.moving else R.string.copying))
        // A copy writes only into the destination; a move also deletes its sources, so a source on a card without a
        // grant is one a grant can help with too.
        val writtenPaths = if (isMove) listOf(path) + selection.map { it.path } else listOf(path)
        viewModelScope.launch {
            try {
                writingTo(writtenPaths) {
                    if (isMove) {
                        directoryModel.moveFile(selection.toMutableList(), path)
                    } else {
                        directoryModel.copyFile(selection.toMutableList(), path)
                    }
                }
                reload()
                _uiEvents.value =
                    Event(DirectoryUiEvent.ShowSnackbar(if (isMove) R.string.moving_successful else R.string.copy_successful))
            } catch (err: FileOperationException) {
                reportFailure(err, errorMessage) { launchTransfer(actionType, selection, path) }
            } finally {
                updateState { it.copy(activeActionCount = it.activeActionCount - 1) }
                // A paste ends copy mode as a move does, so a long press selects again.
                endCopyMode()
            }
        }
    }

    fun confirmAction() {
        val state = requireCurrentState()
        if (!state.isInCopyOrMoveMode) return
        val selection = state.common.selectedFiles
        val path = state.currentPath
        if (hasDestinationConflict(selection, path)) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(transferErrorMessage(state.actionType)))
            return
        }
        when (state.actionType) {
            ActionType.COPY, ActionType.MOVE -> launchTransfer(state.actionType, selection, path)
            else -> Unit
        }
    }

    private fun hasDestinationConflict(selection: List<FileModel>, destinationPath: String): Boolean = selection.any { file ->
        file.isDirectory &&
            (
                destinationPath == file.path ||
                    destinationPath.startsWith(file.path + File.separator)
                )
    }

    fun onRenameOkPressed(fileName: String) {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val fileToRename = selection.last()
        launchAction(
            ActionType.RENAME,
            R.string.renaming,
            R.string.renaming_successful,
            R.string.error_while_renaming
        ) {
            writingTo(listOf(fileToRename.path)) { directoryModel.rename(fileToRename, fileName) }
        }
    }

    fun rename() {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.RenameDialogArgs(selection.last().name)))
    }

    // Nothing is deleted on the tap itself: the confirmation is handed the paths of the selection and only the
    // paths it hands back are deleted. Nothing is kept here, so a confirmation acts on the items it showed even
    // when the screen was recreated or the process restarted in between, and the selection at that time never counts.
    fun requestDelete() {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return
        _uiEvents.value = Event(DirectoryUiEvent.ConfirmDelete(selection.map { it.path }))
    }

    fun confirmDelete(paths: List<String>) {
        if (paths.isEmpty()) return
        val items = paths.map { path -> File(path).let { FileModel(path = path, name = it.name, isDirectory = it.isDirectory) } }
        turnOffOptionPanel()
        clearMultipleSelection()
        launchAction(
            ActionType.DELETE,
            R.string.deleting,
            R.string.deleting_successful,
            R.string.error_while_deleting
        ) {
            writingTo(items.map { it.path }) { directoryModel.delete(items) }
        }
    }

    fun onCreate() {
        updateCommon { it.copy(isCreateMenuExpanded = !it.isCreateMenuExpanded) }
    }

    fun createFolder() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.CreateFolderDialogArgs))
    }

    fun createFile() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.CreateFileDialogArgs))
    }

    fun onFolderCreateOkPressed(folderName: String) {
        turnOffOptionPanel()
        clearMultipleSelection()
        val path = requireCurrentState().currentPathForNavigation
        launchAction(
            ActionType.CREATE_FOLDER,
            R.string.creating,
            R.string.folder_creation_successful,
            R.string.error_when_creating_folder,
            { it.copy(common = it.common.copy(isCreateMenuExpanded = false)) }
        ) {
            writingTo(listOf(path)) { directoryModel.createFolder(path, folderName) }
        }
    }

    fun onFileCreateOkPressed(fileName: String) {
        turnOffOptionPanel()
        clearMultipleSelection()
        val path = requireCurrentState().currentPathForNavigation
        launchAction(
            ActionType.CREATE_FILE,
            R.string.creating,
            R.string.file_creation_successful,
            R.string.error_when_creating_file,
            { it.copy(common = it.common.copy(isCreateMenuExpanded = false)) }
        ) {
            writingTo(listOf(path)) { directoryModel.createFile(path, fileName) }
        }
    }

    fun onFileSearchOkPressed(searchQuery: String) {
        val state = requireCurrentState()
        val previousPath =
            if (state.isSearchMode) state.previousPath else state.currentPath
        updateState { uiState ->
            uiState.copy(
                common = uiState.common.copy(isLoading = true),
                currentPath = "",
                files = emptyList(),
                isSearchMode = true,
                query = searchQuery,
                previousPath = previousPath
            )
        }
        reload()
    }

    fun deviceWideSearch() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.FileSearchDialogArgs))
    }

    fun onFavoriteButtonPressed() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val file = selection.last()
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.AddFavoriteDialogArgs(file.path)))
    }

    // The screen lists its folder when it enters it and after every change it makes; a resume lists nothing again,
    // so a folder that cannot be listed is reported once. A swipe lists it again on request.
    fun onFragmentResume() {
        val state = requireCurrentState()
        if (!state.isInCopyOrMoveMode) {
            val hasVisiblePanel =
                state.common.isOptionsPanelVisible ||
                    state.common.isMoreMenuVisible ||
                    state.common.isCreateMenuExpanded
            if (hasVisiblePanel) turnOffOptionPanel()
            if (state.common.selectedFiles.isNotEmpty()) clearMultipleSelection()
        }
    }
}
