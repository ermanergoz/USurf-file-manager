package com.erman.usurf.home.data

import com.erman.usurf.home.utils.REALM_FIELD_NAME_PATH
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class FavoriteDao(private val realm: Realm) {
    fun getFavorites(): RealmLiveData<Favorite> {
        logd("Get favorites")
        return realm.where(Favorite::class.java).findAllAsync().asLiveData()
    }

    fun addFavorite(
        favoritePath: String,
        favoriteName: String,
    ): Boolean {
        logd("Add favorite")
        if (favoriteName.isEmpty()) {
            return false
        }
        realm.beginTransaction()
        try {
            val favorite: Favorite = realm.createObject(favoritePath)
            favorite.name = favoriteName
            favorite.path = favoritePath
            realm.commitTransaction()
            return true
        } catch (err: RealmPrimaryKeyConstraintException) {
            loge("addFavorite $err")
            realm.cancelTransaction()
            return false
        }
    }

    fun removeFavorite(favoritePath: String): Boolean {
        logd("Remove favorite")
        return try {
            val results =
                realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findAll()
            realm.executeTransaction {
                results.deleteAllFromRealm()
            }
            true
        } catch (err: Error) {
            loge("removeFavorite $err")
            false
        }
    }

    fun renameFavorite(
        favoritePath: String,
        newName: String,
    ): Boolean {
        logd("Rename favorite")
        return try {
            val favoriteToRename =
                realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findFirst()
            realm.beginTransaction()
            favoriteToRename?.name = newName
            realm.commitTransaction()
            true
        } catch (err: Error) {
            loge("renameFavorite $err")
            realm.cancelTransaction()
            false
        }
    }
}
