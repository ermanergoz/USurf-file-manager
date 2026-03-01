package com.erman.usurf.preference.data

import com.erman.usurf.preference.domain.PreferencesRepository

class PreferencesRepositoryImpl(
    private val preferenceProvider: PreferenceProvider,
) : PreferencesRepository {
    override fun getRootAccessPreference(): Boolean = preferenceProvider.getRootAccessPreference()

    override fun setRootAccessPreference(value: Boolean) {
        preferenceProvider.editRootAccessPreference(value)
    }

    override fun getFileSortPreference(): String? = preferenceProvider.getFileSortPreference()

    override fun setFileSortPreference(value: String) {
        preferenceProvider.editFileSortPreference(value)
    }

    override fun getShowHiddenPreference(): Boolean = preferenceProvider.getShowHiddenPreference()

    override fun setShowHiddenPreference(value: Boolean) {
        preferenceProvider.editShowHiddenPreference(value)
    }

    override fun getShowThumbnailsPreference(): Boolean = preferenceProvider.getShowThumbnailsPreference()

    override fun setShowThumbnailsPreference(value: Boolean) {
        preferenceProvider.editShowThumbnailsPreference(value)
    }

    override fun setAscendingOrderPreference(value: Boolean) {
        preferenceProvider.editAscendingOrderPreference(value)
    }

    override fun setDescendingOrderPreference(value: Boolean) {
        preferenceProvider.editDescendingOrderPreference(value)
    }

    override fun getDescendingOrderPreference(): Boolean = preferenceProvider.getDescendingOrderPreference()
}
