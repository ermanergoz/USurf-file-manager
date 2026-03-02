package com.erman.usurf.preference.ui

import androidx.lifecycle.ViewModel
import com.erman.usurf.R
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.preference.data.PreferenceProvider

sealed class RootAccessChangeResult {
    object Applied : RootAccessChangeResult()

    data class Rejected(val messageResId: Int) : RootAccessChangeResult()
}

class PreferencesViewModel(
    private val preferenceProvider: PreferenceProvider,
    private val rootHandler: RootHandler,
) : ViewModel() {
    fun onRootAccessChangeRequested(newValue: Boolean): RootAccessChangeResult {
        return if (rootHandler.isDeviceRooted()) {
            preferenceProvider.editRootAccessPreference(newValue)
            RootAccessChangeResult.Applied
        } else {
            RootAccessChangeResult.Rejected(R.string.su_not_found)
        }
    }

    fun onSortPreferenceChanged(newValue: String) {
        preferenceProvider.editFileSortPreference(newValue)
    }

    fun onShowHiddenPreferenceChanged(newValue: Boolean) {
        preferenceProvider.editShowHiddenPreference(newValue)
    }

    fun onShowThumbnailsPreferenceChanged(newValue: Boolean) {
        preferenceProvider.editShowThumbnailsPreference(newValue)
    }

    fun onAscendingOrderPreferenceChanged(newValue: Boolean) {
        preferenceProvider.editAscendingOrderPreference(newValue)
    }

    fun onDescendingOrderPreferenceChanged(newValue: Boolean) {
        preferenceProvider.editDescendingOrderPreference(newValue)
    }
}
