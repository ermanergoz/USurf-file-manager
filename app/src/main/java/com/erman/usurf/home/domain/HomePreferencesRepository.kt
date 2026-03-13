package com.erman.usurf.home.domain

interface HomePreferencesRepository {
    fun getIsKitkatRemovableStorageWarningDisplayed(): Boolean

    fun setKitkatRemovableStorageWarningDisplayed(displayed: Boolean)
}
