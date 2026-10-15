package com.erman.usurf.storage.domain

interface StoragePathsProvider {
    fun getStorageDirectories(): Set<String>
}
