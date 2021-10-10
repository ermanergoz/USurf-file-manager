package com.erman.usurf.application.data

import com.erman.usurf.application.utils.INITIAL_FAVORITES_LIST
import com.erman.usurf.home.data.Favorite
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.kotlin.createObject
import java.io.File

class ApplicationDao(val realm: Realm) {
    fun addInitialFavorites() {
        val directory = StoragePaths().getStorageDirectories().first()

        for (favoriteName in INITIAL_FAVORITES_LIST) {
            val favoritePath = directory + File.separator + favoriteName

            if (File(favoritePath).exists()) {
                realm.beginTransaction()
                try {
                    val favorite: Favorite = realm.createObject(favoritePath)
                    favorite.name = favoriteName
                    favorite.path = favoritePath
                } catch (err: Error) {
                    loge("addInitialFavorites $err")
                } finally {
                    realm.commitTransaction()
                }
            }
        }
    }
}