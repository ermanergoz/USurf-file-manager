package com.erman.usurf.directory.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class DirectoryViewModel(
    private val directoryModel: DirectoryModel,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel(), CoroutineScope {
    private val isMultiSelectionMode: Boolean
        get() = with(requireCurrentState()) { !isInCopyOrMoveMode && common.selectedFiles.isNotEmpty() }

    private val _uiState = MutableLiveData(DirectoryUiState())
    val uiState: LiveData<DirectoryUiState> = _uiState

    private val _uiEvents = MutableLiveData<Event<DirectoryUiEvent>>()
    val uiEvents: LiveData<Event<DirectoryUiEvent>> = _uiEvents

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

    private fun launchAction(
        actionType: ActionType,
        progressMessageResId: Int,
        successMessageResId: Int,
        errorMessageResId: Int,
        finallyStateTransform: (DirectoryUiState) -> DirectoryUiState = { it },
        block: suspend () -> Unit,
    ) {
        updateState { it.copy(actionType = actionType, activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(progressMessageResId))
        launch {
            try {
                block()
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(successMessageResId))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(errorMessageResId))
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            } finally {
                updateState { state ->
                    val base =
                        state.copy(
                            actionType = ActionType.BROWSE,
                            activeActionCount = state.activeActionCount - 1,
                        )
                    finallyStateTransform(base)
                }
            }
        }
    }

    private fun applySelectionChange(
        state: DirectoryUiState,
        newSelection: List<FileModel>,
        toggledFile: FileModel,
        canRename: Boolean,
    ): DirectoryUiState {
        val updatedCommon = state.common.copy(selectedFiles = newSelection, canRename = canRename)
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
            val canRename = newSelection.size == 1
            if (newSelection.isEmpty()) {
                turnOffOptionPanel()
                clearMultipleSelection()
            } else {
                updateState { uiState -> applySelectionChange(uiState, newSelection, file, canRename) }
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
            val updated = applySelectionChange(uiState, newSelection, file, canRename = true)
            updated.copy(
                common =
                    updated.common.copy(
                        isOptionsPanelVisible = true,
                    ),
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
                    isCreateMenuExpanded = false,
                )
            state.copy(
                common = cleared,
                isInCopyOrMoveMode = false,
                actionType = ActionType.BROWSE,
            )
        }
    }

    fun clearMultipleSelection() {
        val clearedFiles = requireCurrentState().fileList.map { it.copy(isSelected = false) }
        updateState { uiState ->
            uiState.copy(
                common = uiState.common.copy(selectedFiles = emptyList(), canRename = false),
                files = clearedFiles,
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

    fun onBackPressed(): Boolean {
        try {
            if (handleCopyOrMoveBack()) return true
            if (handleOptionsPanelBack()) return true
            if (handleSearchModeBack()) return true
            if (navigateToParent()) return true
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
        return false
    }

    private fun requireNonEmptySelectionAndResetPanel(): List<FileModel>? {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return null
        val selectionCopy = selection.toList()
        turnOffOptionPanel()
        clearMultipleSelection()
        return selectionCopy
    }

    fun setPath(path: String) {
        updateState {
            it.copy(
                common = it.common.copy(isLoading = true),
                currentPath = path,
                files = emptyList(),
                isSearchMode = false,
                query = "",
            )
        }
        loadFileList()
    }

    private fun loadFileList() {
        launch {
            try {
                val path = requireCurrentState().currentPathForNavigation
                if (path.isEmpty()) {
                    updateCommon { it.copy(isLoading = false) }
                    return@launch
                }
                val files = directoryModel.getFileModelsFromDirectory(path)
                updateState { current ->
                    current.copy(
                        common = current.common.copy(isLoading = false),
                        currentPath = path,
                        files = files,
                    )
                }
            } catch (err: Exception) {
                updateCommon { it.copy(isLoading = false) }
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.unable_to_open_directory))
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            }
        }
    }

    private fun loadSearchResults() {
        val query = requireCurrentState().query
        launch {
            try {
                val fileList = directoryModel.getSearchedDeviceFiles(query)
                if (fileList.isEmpty()) {
                    _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.empty_folder))
                }
                updateState {
                    it.copy(
                        common = it.common.copy(isLoading = false, isCreateMenuExpanded = false),
                        files = fileList,
                    )
                }
            } catch (err: Exception) {
                updateCommon { it.copy(isLoading = false) }
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            }
        }
    }

    fun compress() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.CompressDialogArgs))
    }

    private fun refreshFileList() {
        launch {
            val state = requireCurrentState()
            val list =
                if (state.isSearchMode) {
                    directoryModel.getSearchedDeviceFiles(state.query)
                } else {
                    directoryModel.getFileModelsFromDirectory(state.currentPath)
                }
            updateState { it.copy(files = list) }
        }
    }

    fun onSwipeRefresh() {
        updateState { it.copy(isRefreshing = true) }
        launch {
            val state = requireCurrentState()
            val list =
                if (state.isSearchMode) {
                    directoryModel.getSearchedDeviceFiles(state.query)
                } else {
                    directoryModel.getFileModelsFromDirectory(state.currentPath)
                }
            updateState { it.copy(files = list, isRefreshing = false) }
        }
    }

    fun onFileCompressOkPressed(zipNameWithExtension: String) {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(
            ActionType.COMPRESS,
            R.string.compressing,
            R.string.compressing_successful,
            R.string.error_while_compressing,
        ) {
            directoryModel.compressFiles(selection, zipNameWithExtension)
        }
    }

    fun extract() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(
            ActionType.EXTRACT,
            R.string.extracting,
            R.string.extracting_successful,
            R.string.error_while_extracting,
        ) {
            directoryModel.extractFiles(selection.first())
        }
    }

    fun showFileInformation() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        selection.forEach { file ->
            _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.InformationDialogArgs(file)))
        }
    }

    fun share() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.ShareActivityArgs(selection)))
    }

    fun showMoreOption() {
        val state = requireCurrentState()
        if (state.common.isMoreMenuVisible) {
            updateCommon { it.copy(isMoreMenuVisible = false) }
        } else {
            updateCommon { it.copy(isMoreMenuVisible = true) }
        }
    }

    fun copy() {
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
                        isLoading = state.isSearchMode,
                    ),
                isInCopyOrMoveMode = true,
                actionType = ActionType.COPY,
                currentPath = targetPath,
                isSearchMode = false,
                query = "",
                files = clearedFiles,
            )
        }
        if (state.isSearchMode) loadFileList()
    }

    fun move() {
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
                        isLoading = state.isSearchMode,
                    ),
                isInCopyOrMoveMode = true,
                actionType = ActionType.MOVE,
                currentPath = targetPath,
                isSearchMode = false,
                query = "",
                files = clearedFiles,
            )
        }
        if (state.isSearchMode) loadFileList()
    }

    private fun launchCopy(
        selection: List<FileModel>,
        path: String,
    ) {
        if (selection.isEmpty()) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.error_while_copying))
            return
        }
        updateState { it.copy(activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.copying))
        launch {
            try {
                directoryModel.copyFile(selection.toMutableList(), path)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.copy_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.error_while_copying))
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            } finally {
                updateState { it.copy(activeActionCount = it.activeActionCount - 1) }
            }
        }
    }

    private fun launchMove(
        selection: List<FileModel>,
        path: String,
    ) {
        if (selection.isEmpty()) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.error_while_moving))
            return
        }
        updateState { it.copy(activeActionCount = it.activeActionCount + 1) }
        _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.moving))
        launch {
            try {
                directoryModel.moveFile(selection.toMutableList(), path)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.moving_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.error_while_moving))
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            } finally {
                updateState { it.copy(activeActionCount = it.activeActionCount - 1) }
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
            _uiEvents.value = Event(DirectoryUiEvent.ShowSnackbar(R.string.error_while_copying))
            return
        }
        when (state.actionType) {
            ActionType.COPY -> launchCopy(selection, path)
            ActionType.MOVE -> launchMove(selection, path)
            else -> Unit
        }
    }

    private fun hasDestinationConflict(
        selection: List<FileModel>,
        destinationPath: String,
    ): Boolean {
        return selection.any { file ->
            file.isDirectory && (
                destinationPath == file.path ||
                    destinationPath.startsWith(file.path + File.separator)
            )
        }
    }

    fun onRenameOkPressed(fileName: String) {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val fileToRename = selection.last()
        launchAction(
            ActionType.RENAME,
            R.string.renaming,
            R.string.renaming_successful,
            R.string.error_while_renaming,
        ) {
            directoryModel.rename(fileToRename, fileName)
        }
    }

    fun rename() {
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.RenameDialogArgs(selection.last().name)))
    }

    fun delete() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        launchAction(
            ActionType.DELETE,
            R.string.deleting,
            R.string.deleting_successful,
            R.string.error_while_deleting,
        ) {
            directoryModel.delete(selection.toMutableList())
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
            { it.copy(common = it.common.copy(isCreateMenuExpanded = false)) },
        ) {
            directoryModel.createFolder(path, folderName)
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
            { it.copy(common = it.common.copy(isCreateMenuExpanded = false)) },
        ) {
            directoryModel.createFile(path, fileName)
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
            )
        }
        loadSearchResults()
    }

    fun deviceWideSearch() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.FileSearchDialogArgs))
    }

    fun onFavoriteButtonPressed() {
        val selection = requireNonEmptySelectionAndResetPanel() ?: return
        val file = selection.last()
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.AddFavoriteDialogArgs(file.path)))
    }

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
        refreshFileList()
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val job: Job = Job()
}
