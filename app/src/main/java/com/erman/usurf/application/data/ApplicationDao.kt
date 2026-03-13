package com.erman.usurf.application.data

import com.erman.usurf.home.data.Favorite
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.kotlin.createObject
import java.io.File

private val INITIAL_FAVORITES_LIST: List<String> =
    listOf("DCIM", "Documents", "Download", "Movies", "Music", "Pictures")

class ApplicationDao(
    val realm: Realm,
    private val storagePathsProvider: StoragePathsProvider,
) {
    fun addInitialFavorites() {
        val directory: String = storagePathsProvider.getStorageDirectories().firstOrNull() ?: return
        for (favoriteName in INITIAL_FAVORITES_LIST) {
            val favoritePath: String = directory + File.separator + favoriteName
            if (File(favoritePath).exists()) {
                realm.beginTransaction()
                try {
                    val favorite: Favorite = realm.createObject(favoritePath)
                    favorite.name = favoriteName
                    favorite.path = favoritePath
                } catch (err: Exception) {
                    loge(err.localizedMessage ?: UNKNOWN_ERROR)
                } finally {
                    realm.commitTransaction()
                }
            }
        }
    }
}
