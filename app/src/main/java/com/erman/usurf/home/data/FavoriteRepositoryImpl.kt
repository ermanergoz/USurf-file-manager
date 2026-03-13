package com.erman.usurf.home.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.model.FavoriteItem
import io.realm.Realm

class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val realm: Realm,
) : FavoriteRepository {
    override fun getFavorites(): LiveData<List<FavoriteItem>> {
        val result = MediatorLiveData<List<FavoriteItem>>()
        result.addSource(favoriteDao.getFavorites()) { realmResults ->
            val items: List<FavoriteItem> =
                realm.copyFromRealm(realmResults).map { favorite: Favorite ->
                    FavoriteItem(
                        id = favorite.id,
                        name = favorite.name,
                        path = favorite.path,
                    )
                }
            result.value = items
        }
        return result
    }

    override fun addFavorite(
        path: String,
        name: String,
    ): Boolean = favoriteDao.addFavorite(path, name)

    override fun removeFavorite(path: String): Boolean = favoriteDao.removeFavorite(path)

    override fun renameFavorite(
        path: String,
        newName: String,
    ): Boolean = favoriteDao.renameFavorite(path, newName)
}
