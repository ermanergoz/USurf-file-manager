package com.erman.usurf.home.data

import androidx.lifecycle.LiveData
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmException
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

private const val REALM_FIELD_NAME_PATH: String = "path"

/** The favourites kept in the Realm [configuration] opens. Each write opens its own Realm and closes it when done. */
class RealmFavoriteDao(private val configuration: RealmConfiguration) : FavoriteDao {
    override fun getFavorites(): LiveData<List<Favorite>> = RealmLiveData(configuration) { realm -> realm.where<Favorite>().findAllAsync() }

    override fun addFavorite(favoritePath: String, favoriteName: String): Boolean {
        if (favoriteName.isEmpty()) {
            return false
        }
        return write { realm ->
            val favorite: Favorite = realm.createObject(favoritePath)
            favorite.name = favoriteName
            favorite.path = favoritePath
            true
        }
    }

    override fun removeFavorite(favoritePath: String): Boolean = write { realm ->
        realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findAll().deleteAllFromRealm()
        true
    }

    override fun renameFavorite(favoritePath: String, newName: String): Boolean = write { realm ->
        val favorite: Favorite? = realm.where<Favorite>().equalTo(REALM_FIELD_NAME_PATH, favoritePath).findFirst()
        favorite?.name = newName
        favorite != null
    }

    // [change] answers whether it found what it was asked to change. A write Realm refused, or one made where Realm
    // cannot write (the main thread), changes nothing: the transaction is cancelled and the caller is told it failed.
    private fun write(change: (Realm) -> Boolean): Boolean = try {
        Realm.getInstance(configuration).use { realm ->
            var changed = false
            realm.executeTransaction { changed = change(it) }
            changed
        }
    } catch (err: RealmPrimaryKeyConstraintException) {
        // The folder is a favourite already.
        failedWrite(err)
    } catch (err: RealmException) {
        failedWrite(err)
    } catch (err: IllegalStateException) {
        failedWrite(err)
    }

    private fun failedWrite(err: RuntimeException): Boolean {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        return false
    }
}
