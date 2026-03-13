package com.erman.usurf.application.data

import com.erman.usurf.application.domain.ApplicationPreferencesRepository

class ApplicationPreferencesRepositoryImpl(
    private val applicationPreferenceProvider: ApplicationPreferenceProvider,
) : ApplicationPreferencesRepository {
    override fun getIsFirstLaunch(): Boolean = applicationPreferenceProvider.getIsFirstLaunch()

    override fun setIsFirstLaunch(isFirstLaunch: Boolean) {
        applicationPreferenceProvider.editIsFirstLaunch(isFirstLaunch)
    }
}
