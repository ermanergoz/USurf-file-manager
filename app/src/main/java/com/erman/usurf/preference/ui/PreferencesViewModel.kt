package com.erman.usurf.preference.ui

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erman.usurf.R
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.preference.domain.RootAccessChecker
import com.erman.usurf.utils.Event
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class RootAccessChangeResult {
    data class Applied(val isRootAccessEnabled: Boolean) : RootAccessChangeResult()

    data class Rejected(@StringRes val messageResId: Int) : RootAccessChangeResult()
}

class PreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val rootAccessChecker: RootAccessChecker,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _isRootAccessCheckPending = MutableLiveData(false)
    val isRootAccessCheckPending: LiveData<Boolean> = _isRootAccessCheckPending

    private val _rootAccessChangeResult = MutableLiveData<Event<RootAccessChangeResult>>()
    val rootAccessChangeResult: LiveData<Event<RootAccessChangeResult>> = _rootAccessChangeResult

    fun onRootAccessChangeRequested(newValue: Boolean) {
        if (!newValue) {
            preferencesRepository.setRootAccessPreference(false)
            _rootAccessChangeResult.value = Event(RootAccessChangeResult.Applied(isRootAccessEnabled = false))
            return
        }
        _isRootAccessCheckPending.value = true
        viewModelScope.launch {
            // The su request waits for the superuser prompt to be answered, so it never runs on main.
            val result: RootAccessChangeResult = withContext(ioDispatcher) { checkRootAccess() }
            if (result is RootAccessChangeResult.Applied) {
                preferencesRepository.setRootAccessPreference(true)
            }
            _isRootAccessCheckPending.value = false
            _rootAccessChangeResult.value = Event(result)
        }
    }

    private fun checkRootAccess(): RootAccessChangeResult = when {
        !rootAccessChecker.isDeviceRooted() -> RootAccessChangeResult.Rejected(R.string.su_not_found)
        !rootAccessChecker.isRootAccessGiven() -> RootAccessChangeResult.Rejected(R.string.root_access_denied)
        else -> RootAccessChangeResult.Applied(isRootAccessEnabled = true)
    }

    fun onSortPreferenceChanged(newValue: String) {
        preferencesRepository.setFileSortPreference(newValue)
    }

    fun onShowHiddenPreferenceChanged(newValue: Boolean) {
        preferencesRepository.setShowHiddenPreference(newValue)
    }

    fun onShowThumbnailsPreferenceChanged(newValue: Boolean) {
        preferencesRepository.setShowThumbnailsPreference(newValue)
    }

    fun onAscendingOrderPreferenceChanged(newValue: Boolean) {
        preferencesRepository.setAscendingOrderPreference(newValue)
    }

    fun onDescendingOrderPreferenceChanged(newValue: Boolean) {
        preferencesRepository.setDescendingOrderPreference(newValue)
    }
}
