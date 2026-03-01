package com.erman.usurf.preference.ui

import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.preference.domain.PreferencesRepository

sealed class RootAccessChangeResult {
    object Applied : RootAccessChangeResult()

    data class Rejected(val messageResId: Int) : RootAccessChangeResult()
}

class PreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val rootHandler: RootHandler,
) : ViewModel() {
    fun onRootAccessChangeRequested(newValue: Boolean): RootAccessChangeResult {
        if (!newValue) {
            preferencesRepository.setRootAccessPreference(false)
            return RootAccessChangeResult.Applied
        }
        if (!rootHandler.isDeviceRooted()) {
            return RootAccessChangeResult.Rejected(R.string.su_not_found)
        }
        if (!rootHandler.isRootAccessGiven()) {
            return RootAccessChangeResult.Rejected(R.string.root_access_denied)
        }
        preferencesRepository.setRootAccessPreference(true)
        return RootAccessChangeResult.Applied
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
