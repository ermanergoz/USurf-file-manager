package com.erman.usurf.home.data

import android.widget.TextView
import android.widget.Toast
import com.erman.usurf.R
import com.erman.usurf.application.MainApplication.Companion.appContext
import com.erman.usurf.home.utils.REALM_FIELD_NAME_PATH
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class FavoriteDao(var realm: Realm) {
    fun getFavorites(): RealmLiveData<Favorite> {
        logd("Get favorites")
        return realm.where(Favorite::class.java).findAllAsync().asLiveData()
    }

    fun addFavorite(favoritePath: String, favoriteName: String): Boolean {
        logd("Add favorite")
        realm.beginTransaction()
        try {
            if(favoriteName.isNotEmpty()) {
                val favorite: Favorite = realm.createObject(favoritePath)
                favorite.name = favoriteName
                favorite.path = favoritePath
            } else {
                displayToast(R.string.unable_to_create_favorite_no_name)
                return false
            }
        } catch (err: RealmPrimaryKeyConstraintException) {
            loge("addFavorite $err")
            displayToast(R.string.unable_to_create_favorite)
            return false
        } finally {
            realm.commitTransaction()
        }
        displayToast(R.string.favorite_created)
        return true
    }

    fun removeFavorite(favorite: TextView): Boolean {
        logd("Remove favorite")
        try {
            val results = realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favorite.tag.toString()).findAllAsync()
            realm.executeTransaction {
                results.deleteFirstFromRealm()
            }
        } catch (err: Error) {
            loge("removeFavorite $err")
            displayToast(R.string.unable_to_delete_favorite)
            return false
        }
        displayToast(R.string.favorite_deleted)
        return true
    }

    fun renameFavorite(favorite: TextView, newName: String): Boolean {
        logd("Rename favorite")
        try {
            val favoriteToRename = realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favorite.tag.toString()).findFirst()
            realm.beginTransaction()
            favoriteToRename?.let { it.name = newName }
        } catch (err: Error) {
            loge("renameFavorite $err")
            displayToast(R.string.unable_to_rename_favorite)
            return false
        } finally {
            realm.commitTransaction()
        }
        displayToast(R.string.favorite_renamed)
        return true
    }

    private fun displayToast(messageId: Int) {
        Toast.makeText(appContext, appContext.getString(messageId), Toast.LENGTH_LONG).show()
    }
}