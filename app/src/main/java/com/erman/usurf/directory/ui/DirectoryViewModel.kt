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
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
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

    // Every failure of the directory model reaches here as a FileOperationException, the one type caught on this
    // screen; anything else is a programming error and is left to surface.
    private fun launchAction(
        actionType: ActionType,
        finallyStateTransform: (DirectoryUiState) -> DirectoryUiState = { it },
        block: suspend () -> Unit
    ) {
        actionAwaitingGrant = null
        updateState { it.copy(actionType = actionType, activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(actionType.progressMessage))
        viewModelScope.launch {
            try {
                block()
                reload()
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(actionType.successMessage))
            } catch (err: FileOperationException) {
                reportFailure(err, actionType.failureMessage) { launchAction(actionType, finallyStateTransform, block) }
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

    fun onFileClick(file: FileModel) {
        if (isMultiSelectionMode) {
            updateState { it.tapWhileSelecting(file) }
        } else {
            if (file.isDirectory) {
                setPath(file.path)
            } else {
                _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.OpenFileActivityArgs(file.path)))
            }
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        if (requireCurrentState().isInCopyOrMoveMode) return false
        updateState { it.select(file).openPanel() }
        return true
    }

    fun endCopyMode() {
        updateState(DirectoryUiState::reset)
    }

    /** Answers Back: true when the screen handled it, false when Back leaves the screen. */
    fun onBackPressed(): Boolean {
        when (val action = BackNavigationPolicy.decide(requireCurrentState(), preferencesRepository.getRootAccessPreference())) {
            BackAction.ClosePanel -> updateState(DirectoryUiState::reset)
            is BackAction.LeaveSearch -> setPath(action.path)
            is BackAction.GoUp -> setPath(action.path)
            BackAction.Stay -> Unit
            BackAction.Leave -> return false
        }
        return true
    }

    fun shouldConfirmExit(): Boolean = requireCurrentState().isActionInProgress

    private fun requireNonEmptySelectionAndResetPanel(): List<FileModel>? {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return null
        updateState(DirectoryUiState::reset)
        return selection
    }

    fun setPath(path: String) {
        actionAwaitingGrant = null
        updateState {
            it.copy(
                common = it.common.copy(isLoading = true),
                currentPath = path,
                files = emptyList(),
                isSearchMode = false,
                query = "",
                readableParent = null
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
                // The way up is stored before the listing, so Back finds it even when the folder cannot be listed.
                val readableParent = if (state.isSearchMode) null else directoryModel.readableParentOf(state.currentPath)
                if (!requireCurrentState().showsLocationOf(state)) return@launch
                updateState { it.copy(readableParent = readableParent) }
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
        launchAction(ActionType.COMPRESS) {
            // The archive is written into the folder of the selection.
            writingTo(listOf(selection.first().folderPath)) { archiveOperations.compressFiles(selection, zipNameWithExtension) }
        }
    }

    fun extract() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(ActionType.EXTRACT) {
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

    fun toggleMoreMenu() {
        updateState(DirectoryUiState::toggleMoreMenu)
    }

    /** Starts browsing for where to paste the selection, as a copy or a move by [actionType]. */
    fun enterTransferMode(actionType: ActionType) {
        val state = requireCurrentState()
        if (state.isInCopyOrMoveMode) return
        updateState { it.enterTransfer(actionType) }
        if (state.isSearchMode) reload()
    }

    fun confirmAction() {
        val state = requireCurrentState()
        val actionType = state.actionType
        if (!state.isInCopyOrMoveMode || (actionType != ActionType.COPY && actionType != ActionType.MOVE)) return
        val selection = state.common.selectedFiles
        val destination = state.currentPath
        if (selection.isEmpty() || hasDestinationConflict(selection, destination)) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(actionType.failureMessage))
            return
        }
        // A paste ends copy mode as a move does, whether it succeeds or fails, so a long press selects again.
        launchAction(actionType, DirectoryUiState::reset) {
            val isMove = actionType == ActionType.MOVE
            // A copy writes only into the destination; a move also deletes its sources, so a source on a card without a
            // grant is one a grant can help with too.
            val writtenPaths = if (isMove) listOf(destination) + selection.map { it.path } else listOf(destination)
            writingTo(writtenPaths) {
                if (isMove) {
                    directoryModel.moveFile(selection, destination)
                } else {
                    directoryModel.copyFile(selection, destination)
                }
            }
        }
    }

    fun onRenameOkPressed(fileName: String) {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val fileToRename = selection.last()
        launchAction(ActionType.RENAME) {
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
        updateState(DirectoryUiState::reset)
        launchAction(ActionType.DELETE) {
            writingTo(paths) { directoryModel.delete(paths) }
        }
    }

    fun toggleCreateMenu() {
        updateState(DirectoryUiState::toggleCreateMenu)
    }

    /** Asks for the name of a new folder or file, by [kind]. */
    fun requestCreate(kind: CreateKind) {
        val dialogArgs =
            when (kind) {
                CreateKind.FOLDER -> DialogArgs.CreateFolderDialogArgs
                CreateKind.FILE -> DialogArgs.CreateFileDialogArgs
            }
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(dialogArgs))
    }

    /** Creates a folder or a file, by [kind], named [name] in the folder on screen. */
    fun onCreateNameEntered(kind: CreateKind, name: String) {
        updateState(DirectoryUiState::reset)
        val path = requireCurrentState().currentPathForNavigation
        launchAction(kind.actionType, DirectoryUiState::closeCreateMenu) {
            writingTo(listOf(path)) {
                when (kind) {
                    CreateKind.FOLDER -> directoryModel.createFolder(path, name)
                    CreateKind.FILE -> directoryModel.createFile(path, name)
                }
            }
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
                previousPath = previousPath,
                readableParent = null
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
    fun onScreenResumed() {
        updateState(DirectoryUiState::resumed)
    }
}
