package com.erman.usurf.home.data

import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.exceptions.RealmException
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

private const val REALM_FIELD_NAME_PATH: String = "path"

class FavoriteDao(private val realm: Realm) {
    fun getFavorites(): RealmLiveData<Favorite> = realm.where(Favorite::class.java).findAllAsync().asLiveData()

    fun addFavorite(favoritePath: String, favoriteName: String): Boolean {
        if (favoriteName.isEmpty()) {
            return false
        }
        return try {
            realm.beginTransaction()
            val favorite: Favorite = realm.createObject(favoritePath)
            favorite.name = favoriteName
            favorite.path = favoritePath
            realm.commitTransaction()
            true
        } catch (err: RealmPrimaryKeyConstraintException) {
            // The folder is a favourite already.
            failedWrite(err)
        } catch (err: RealmException) {
            failedWrite(err)
        } catch (err: IllegalStateException) {
            failedWrite(err)
        }
    }

    fun removeFavorite(favoritePath: String): Boolean = try {
        val results =
            realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findAll()
        realm.executeTransaction {
            results.deleteAllFromRealm()
        }
        true
    } catch (err: RealmException) {
        failedWrite(err)
    } catch (err: IllegalStateException) {
        failedWrite(err)
    }

    fun renameFavorite(favoritePath: String, newName: String): Boolean = try {
        val favoriteToRename =
            realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findFirst()
        realm.beginTransaction()
        favoriteToRename?.name = newName
        realm.commitTransaction()
        true
    } catch (err: RealmException) {
        failedWrite(err)
    } catch (err: IllegalStateException) {
        failedWrite(err)
    }

    // A write Realm refused, or one made where Realm cannot write (a closed instance, another thread), changes nothing:
    // its transaction, when one is open, is cancelled and the caller is told it failed.
    private fun failedWrite(err: RuntimeException): Boolean {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        if (realm.isInTransaction) realm.cancelTransaction()
        return false
    }
}
