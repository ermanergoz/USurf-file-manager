package com.erman.usurf.storage.data

import com.erman.usurf.storage.domain.StorageDirectoryRepository

class StorageDirectoryRepositoryImpl(
    private val storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider,
) : StorageDirectoryRepository {
    override fun getChosenUri(): String? = storageDirectoryPreferenceProvider.getChosenUri()

    override fun setChosenUri(uri: String) {
        storageDirectoryPreferenceProvider.editChosenUri(uri)
    }
}
