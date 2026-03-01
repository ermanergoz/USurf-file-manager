package com.erman.usurf.home.data

import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

private const val REALM_FIELD_NAME_PATH: String = "path"

class FavoriteDao(private val realm: Realm) {
    fun getFavorites(): RealmLiveData<Favorite> {
        return realm.where(Favorite::class.java).findAllAsync().asLiveData()
    }

    fun addFavorite(
        favoritePath: String,
        favoriteName: String,
    ): Boolean {
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
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            realm.cancelTransaction()
            return false
        }
    }

    fun removeFavorite(favoritePath: String): Boolean {
        return try {
            val results =
                realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findAll()
            realm.executeTransaction {
                results.deleteAllFromRealm()
            }
            true
        } catch (err: Error) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            false
        }
    }

    fun renameFavorite(
        favoritePath: String,
        newName: String,
    ): Boolean {
        return try {
            val favoriteToRename =
                realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findFirst()
            realm.beginTransaction()
            favoriteToRename?.name = newName
            realm.commitTransaction()
            true
        } catch (err: Error) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            realm.cancelTransaction()
            false
        }
    }
}
