package com.erman.usurf.storage.domain

interface StorageDirectoryRepository {
    fun getChosenUri(): String?

    fun setChosenUri(uri: String)
}
