package com.erman.usurf.home.data

import com.erman.usurf.home.domain.HomePreferencesRepository

class HomePreferencesRepositoryImpl(
    private val homePreferenceProvider: HomePreferenceProvider,
) : HomePreferencesRepository {
    override fun getIsKitkatRemovableStorageWarningDisplayed(): Boolean =
        homePreferenceProvider.getIsKitkatRemovableStorageWarningDisplayedPreference()

    override fun setKitkatRemovableStorageWarningDisplayed(displayed: Boolean) {
        homePreferenceProvider.editIsKitkatRemovableStorageWarningDisplayedPreference(displayed)
    }
}
