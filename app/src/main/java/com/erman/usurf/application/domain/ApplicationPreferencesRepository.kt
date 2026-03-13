package com.erman.usurf.application.domain

interface ApplicationPreferencesRepository {
    fun getIsFirstLaunch(): Boolean

    fun setIsFirstLaunch(isFirstLaunch: Boolean)
}
