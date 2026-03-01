package com.erman.usurf.directory.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.utils.Event
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class DirectoryViewModel(
    private val directoryModel: DirectoryModel,
    private val preferenceProvider: PreferenceProvider,
) : ViewModel(), CoroutineScope {

    private var multiSelectionMode: Boolean = false

    private val _uiState = MutableLiveData<DirectoryUIState>(DirectoryUIState.Browsing())
    val uiState: LiveData<DirectoryUIState> = _uiState

    private val _uiEvents = MutableLiveData<Event<DirectoryUiEvent>>()
    val uiEvents: LiveData<Event<DirectoryUiEvent>> = _uiEvents

    private fun updateState(transform: (DirectoryUIState) -> DirectoryUIState) {
        _uiState.value = transform(requireCurrentState())
    }

    private fun requireCurrentState(): DirectoryUIState =
        _uiState.value ?: DirectoryUIState.Browsing()

    private fun updateCommon(transform: (CommonUi) -> CommonUi) {
        updateState { state ->
            when (state) {
                is DirectoryUIState.Browsing -> state.copy(common = transform(state.common))
                is DirectoryUIState.Searching -> state.copy(common = transform(state.common))
                is DirectoryUIState.ExecutingOperation.FileAction -> state.copy(common = transform(state.common))
            }
        }
    }

    fun onFileClick(file: FileModel) {
        val state = requireCurrentState()
        val common = state.common
        if (multiSelectionMode) {
            val newSelection = directoryModel.manageMultipleSelectionList(file, common.selectedFiles.toMutableList())
            val canRename = newSelection.size == 1
            if (newSelection.isEmpty()) {
                turnOffOptionPanel()
                clearMultipleSelection()
            } else {
                updateCommon { it.copy(selectedFiles = newSelection, canRename = canRename) }
            }
            updateCommon { it.copy(isInRoot = file.isInRoot) }
        } else {
            if (file.isDirectory) {
                setPath(file.path)
                updateCommon { it.copy(isInRoot = file.isInRoot) }
            } else {
                _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.OpenFileActivityArgs(file.path)))
                updateCommon { it.copy(isInRoot = file.isInRoot) }
            }
        }
    }

    fun onFileLongClick(file: FileModel): Boolean {
        val state = requireCurrentState()
        if (state.isInCopyOrMoveMode) return false
        val newSelection = directoryModel.manageMultipleSelectionList(file, state.common.selectedFiles.toMutableList())
        updateCommon {
            it.copy(
                selectedFiles = newSelection,
                isInRoot = file.isInRoot,
                canRename = true,
                isOptionsPanelVisible = true,
            )
        }
        multiSelectionMode = true
        return true
    }

    fun turnOffOptionPanel() {
        updateState { state ->
            val cleared = state.common.copy(
                isOptionsPanelVisible = false,
                isMoreMenuVisible = false,
                selectedFiles = emptyList(),
                canRename = false,
            )
            when (state) {
                is DirectoryUIState.Browsing -> state.copy(common = cleared)
                is DirectoryUIState.Searching -> state.copy(common = cleared)
                is DirectoryUIState.ExecutingOperation.FileAction -> {
                    DirectoryUIState.Browsing(
                        common = cleared.copy(isOptionsPanelVisible = false),
                        currentPath = state.currentPath,
                        files = state.files,
                    )
                }
            }
        }
        multiSelectionMode = false
    }

    fun clearMultipleSelection() {
        val state = requireCurrentState()
        val newSelection = directoryModel.clearMultipleSelection(state.common.selectedFiles.toMutableList())
        updateCommon { it.copy(selectedFiles = newSelection, canRename = false) }
    }

    fun endCopyMode() {
        turnOffOptionPanel()
        clearMultipleSelection()
    }

    fun onBackPressed(): Boolean {
        try {
            val state = requireCurrentState()
            val c = state.common
            val inOptionOrMenu = (c.isOptionsPanelVisible && !state.isInCopyOrMoveMode) || c.isCreateMenuExpanded
            if (inOptionOrMenu) {
                turnOffOptionPanel()
                clearMultipleSelection()
                return true
            }
            if (state.isSearchMode) {
                val prevPath = (state as DirectoryUIState.Searching).previousPath
                setPath(prevPath.ifEmpty { ROOT_DIRECTORY })
                return true
            }
            val currentPath = state.currentPathForNavigation
            File(currentPath).parent?.let { parent ->
                val prevFile = File(parent)
                val canNavigate = (prevFile.canRead() && prevFile.path != ROOT_DIRECTORY) ||
                    preferenceProvider.getRootAccessPreference()
                if (canNavigate) {
                    setPath(prevFile.path)
                    return true
                }
            }
        } catch (err: Exception) {
            err.printStackTrace()
            loge("onBackPressed $err")
        }
        return false
    }

    fun setPath(path: String) {
        updateState {
            when (it) {
                is DirectoryUIState.Browsing -> it.copy(currentPath = path, files = emptyList())
                is DirectoryUIState.Searching -> DirectoryUIState.Browsing(it.common, path, emptyList())
                is DirectoryUIState.ExecutingOperation.FileAction -> DirectoryUIState.Browsing(it.common, path, emptyList())
            }
        }
        getFileList()
    }

    private fun getFileList() {
        try {
            updateState { state ->
                val loadingCommon = state.common.copy(isLoading = true)
                when (state) {
                    is DirectoryUIState.Browsing -> state.copy(common = loadingCommon)
                    is DirectoryUIState.Searching -> state.copy(common = loadingCommon)
                    is DirectoryUIState.ExecutingOperation.FileAction -> state.copy(common = loadingCommon)
                }
            }
            launch {
                try {
                    val state = requireCurrentState()
                    val path = state.currentPathForNavigation
                    if (path.isEmpty()) return@launch
                    val directory = directoryModel.getFileModelsFromFiles(path)
                    if (directory.isEmpty()) {
                        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.empty_folder))
                    }
                    updateState { current ->
                        val doneCommon = current.common.copy(isLoading = false)
                        when (current) {
                            is DirectoryUIState.Browsing -> current.copy(common = doneCommon, files = directory)
                            is DirectoryUIState.Searching -> current.copy(common = doneCommon)
                            is DirectoryUIState.ExecutingOperation.FileAction ->
                                current.copy(common = doneCommon, currentPath = path, files = directory)
                        }
                    }
                } finally {
                    updateCommon { it.copy(isLoading = false) }
                }
            }
        } catch (err: IllegalStateException) {
            _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.unable_to_open_directory))
            updateCommon { it.copy(isLoading = false) }
            loge("getFileList $err")
        }
    }

    private fun getSearchedFiles() {
        val state = requireCurrentState()
        if (state !is DirectoryUIState.Searching) return
        val query = state.query
        updateState {
            when (it) {
                is DirectoryUIState.Searching -> it.copy(common = it.common.copy(isLoading = true))
                else -> it
            }
        }
        launch {
            val fileList = directoryModel.getSearchedDeviceFiles(query)
            if (fileList.isEmpty()) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.empty_folder))
            }
            updateState {
                when (it) {
                    is DirectoryUIState.Searching ->
                        it.copy(
                            common = it.common.copy(isLoading = false, isCreateMenuExpanded = false),
                            results = fileList,
                        )
                    else -> it
                }
            }
        }
    }

    fun compress() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.CompressDialogArgs))
    }

    private fun refreshFileList() {
        launch {
            when (val state = requireCurrentState()) {
                is DirectoryUIState.Searching -> {
                    val list = directoryModel.getSearchedDeviceFiles(state.query)
                    updateState { if (it is DirectoryUIState.Searching) it.copy(results = list) else it }
                }
                is DirectoryUIState.Browsing -> {
                    val list = directoryModel.getFileModelsFromFiles(state.currentPath)
                    updateState { if (it is DirectoryUIState.Browsing) it.copy(files = list) else it }
                }
                is DirectoryUIState.ExecutingOperation.FileAction -> {
                    val list = directoryModel.getFileModelsFromFiles(state.currentPath)
                    updateState {
                        if (it is DirectoryUIState.ExecutingOperation.FileAction) it.copy(files = list) else it
                    }
                }
            }
        }
    }

    fun onFileCompressOkPressed(zipNameWithExtension: String) {
        turnOffOptionPanel()
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.compressing))
        logd("onFileCompressOkPressed")
        launch {
            try {
                val selection = requireCurrentState().common.selectedFiles
                directoryModel.compressFiles(selection.toMutableList(), zipNameWithExtension)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.compressing_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_compressing))
                loge("onFileCompressOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun extract() {
        turnOffOptionPanel()
        val selection = requireCurrentState().common.selectedFiles
        if (selection.isEmpty()) return
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.extracting))
        logd("extract")
        launch {
            try {
                directoryModel.extractFiles(selection.first())
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.extracting_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_extracting))
                loge("extract $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun showFileInformation() {
        requireCurrentState().common.selectedFiles.forEach { file ->
            _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.InformationDialogArgs(file)))
        }
    }

    fun share() {
        val selection = requireCurrentState().common.selectedFiles
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.ShareActivityArgs(selection)))
    }

    fun showMoreOption() {
        updateCommon { it.copy(isMoreMenuVisible = !it.isMoreMenuVisible) }
    }

    fun copy() {
        when (val state = requireCurrentState()) {
            is DirectoryUIState.Browsing -> {
                _uiState.value = DirectoryUIState.ExecutingOperation.FileAction(
                    common = state.common.copy(
                        isOptionsPanelVisible = false,
                        isMoreMenuVisible = false,
                    ),
                    actionType = ActionType.COPY,
                    currentPath = state.currentPath,
                    files = state.files,
                )
            }
            is DirectoryUIState.Searching -> {
                _uiState.value = DirectoryUIState.ExecutingOperation.FileAction(
                    common = state.common.copy(
                        isOptionsPanelVisible = false,
                        isMoreMenuVisible = false,
                    ),
                    actionType = ActionType.COPY,
                    currentPath = state.previousPath,
                    files = emptyList(),
                )
                getFileList()
            }
            is DirectoryUIState.ExecutingOperation.FileAction -> { }
        }
        multiSelectionMode = false
    }

    fun move() {
        when (val state = requireCurrentState()) {
            is DirectoryUIState.Browsing -> {
                _uiState.value = DirectoryUIState.ExecutingOperation.FileAction(
                    common = state.common.copy(
                        isOptionsPanelVisible = false,
                        isMoreMenuVisible = false,
                    ),
                    actionType = ActionType.MOVE,
                    currentPath = state.currentPath,
                    files = state.files,
                )
            }
            is DirectoryUIState.Searching -> {
                _uiState.value = DirectoryUIState.ExecutingOperation.FileAction(
                    common = state.common.copy(
                        isOptionsPanelVisible = false,
                        isMoreMenuVisible = false,
                    ),
                    actionType = ActionType.MOVE,
                    currentPath = state.previousPath,
                    files = emptyList(),
                )
                getFileList()
            }
            is DirectoryUIState.ExecutingOperation.FileAction -> { }
        }
        multiSelectionMode = false
    }

    private fun launchCopy() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.copying))
        logd("copy - copyMode")
        launch {
            try {
                val state = requireCurrentState()
                if (state is DirectoryUIState.ExecutingOperation.FileAction) {
                    val path = state.currentPath
                    val selection = state.common.selectedFiles
                    directoryModel.copyFile(selection.toMutableList(), path)
                    refreshFileList()
                    _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.copy_successful))
                }
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_copying))
                loge("confirmAction-copyMode $err")
            }
        }
    }

    private fun launchMove() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.moving))
        logd("move - moveMode")
        launch {
            try {
                val state = requireCurrentState()
                if (state is DirectoryUIState.ExecutingOperation.FileAction) {
                    val selection = state.common.selectedFiles
                    val path = state.currentPath
                    directoryModel.moveFile(selection.toMutableList(), path)
                    refreshFileList()
                    _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.moving_successful))
                }
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_moving))
                loge("confirmAction-moveMode $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun confirmAction() {
        when (val state = requireCurrentState()) {
            is DirectoryUIState.ExecutingOperation.FileAction -> {
                when (state.actionType) {
                    ActionType.COPY -> launchCopy()
                    ActionType.MOVE -> {
                        turnOffOptionPanel()
                        launchMove()
                    }
                }
            }
            else -> { }
        }
    }

    fun onRenameOkPressed(fileName: String) {
        turnOffOptionPanel()
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.renaming))
        logd("onRenameOkPressed")
        launch {
            try {
                val selection = requireCurrentState().common.selectedFiles
                directoryModel.rename(selection.last(), fileName)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.renaming_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_renaming))
                loge("onRenameOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun rename() {
        val selection = requireCurrentState().common.selectedFiles
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.RenameDialogArgs(selection.last().name)))
    }

    fun delete() {
        turnOffOptionPanel()
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.deleting))
        logd("delete")
        launch {
            try {
                val selection = requireCurrentState().common.selectedFiles
                directoryModel.delete(selection.toMutableList())
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.deleting_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_while_deleting))
                loge("delete $err")
            } finally {
                clearMultipleSelection()
            }
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
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.creating))
        logd("onFolderCreateOkPressed")
        launch {
            try {
                val path = requireCurrentState().currentPathForNavigation
                directoryModel.createFolder(path, folderName)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.folder_creation_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_when_creating_folder))
                loge("onFolderCreateOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun onFileCreateOkPressed(fileName: String) {
        turnOffOptionPanel()
        _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.creating))
        logd("onFileCreateOkPressed")
        launch {
            try {
                val path = requireCurrentState().currentPathForNavigation
                directoryModel.createFile(path, fileName)
                refreshFileList()
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.file_creation_successful))
            } catch (err: CancellationException) {
                _uiEvents.value = Event(DirectoryUiEvent.ShowToast(R.string.error_when_creating_file))
                loge("onFileCreateOkPressed $err")
            } finally {
                clearMultipleSelection()
            }
        }
    }

    fun onFileSearchOkPressed(searchQuery: String) {
        val state = requireCurrentState()
        val (baseCommon, previousPath) = when (state) {
            is DirectoryUIState.Browsing -> state.common to state.currentPath
            is DirectoryUIState.Searching -> state.common to state.previousPath
            is DirectoryUIState.ExecutingOperation.FileAction -> state.common to state.currentPath
        }
        _uiState.value = DirectoryUIState.Searching(
            common = baseCommon,
            query = searchQuery,
            results = emptyList(),
            previousPath = previousPath,
        )
        getSearchedFiles()
    }

    fun deviceWideSearch() {
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.FileSearchDialogArgs))
    }

    fun onFavoriteButtonPressed() {
        logd("favoriteButton")
        val selection = requireCurrentState().common.selectedFiles
        val file = selection.last()
        _uiEvents.value = Event(DirectoryUiEvent.ShowDialog(DialogArgs.AddFavoriteDialogArgs(file.path)))
    }

    fun onFragmentResume() {
        val state = requireCurrentState()
        if (!state.isInCopyOrMoveMode) {
            turnOffOptionPanel()
            clearMultipleSelection()
        }
        getFileList()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val job = Job()
}
