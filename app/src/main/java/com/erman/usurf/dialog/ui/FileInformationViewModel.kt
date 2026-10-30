package com.erman.usurf.dialog.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erman.usurf.directory.domain.FolderSizeRepository
import com.erman.usurf.directory.model.FileOperationException
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import kotlinx.coroutines.launch

/** The state of one information dialog: the sizes of the folders it shows, kept while the dialog is made again. */
class FileInformationViewModel(private val folderSizeRepository: FolderSizeRepository) : ViewModel() {
    private val folderSizes: MutableMap<String, LiveData<Long>> = mutableMapOf()

    /**
     * The size of the folder at [path] in bytes. Its walk runs once for this dialog, however often a page or a dialog made
     * again after a rotation asks for it; a folder that cannot be read holds 0.
     */
    fun folderSize(path: String): LiveData<Long> = folderSizes.getOrPut(path) {
        MutableLiveData<Long>().also { size ->
            viewModelScope.launch {
                size.value =
                    try {
                        folderSizeRepository.folderSizeOf(path)
                    } catch (err: FileOperationException) {
                        loge(err.localizedMessage ?: UNKNOWN_ERROR)
                        0L
                    }
            }
        }
    }
}
