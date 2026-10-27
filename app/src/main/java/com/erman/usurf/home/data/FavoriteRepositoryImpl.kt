package com.erman.usurf.home.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.FavoriteKind
import io.realm.Realm
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher

class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val realm: Realm,
    private val ioDispatcher: CoroutineDispatcher
) : FavoriteRepository {
    override fun getFavorites(): LiveData<List<FavoriteItem>> =
        favoritesWithKinds(favoriteDao.getFavorites().map { results -> realm.copyFromRealm(results) }, ioDispatcher, ::favoriteKindOf)

    override fun addFavorite(path: String, name: String): Boolean = favoriteDao.addFavorite(path, name)

    override fun removeFavorite(path: String): Boolean = favoriteDao.removeFavorite(path)

    override fun renameFavorite(path: String, newName: String): Boolean = favoriteDao.renameFavorite(path, newName)
}

/**
 * Each of [favorites] with the kind [kindOf] reads from its path, on [ioDispatcher]: one file system call per favourite,
 * never on the main thread. A later list replaces one still being read.
 */
fun favoritesWithKinds(
    favorites: LiveData<List<Favorite>>,
    ioDispatcher: CoroutineDispatcher,
    kindOf: (String) -> FavoriteKind
): LiveData<List<FavoriteItem>> = favorites.switchMap { list ->
    liveData(ioDispatcher) {
        emit(list.map { favorite -> FavoriteItem(favorite.id, favorite.name, favorite.path, kindOf(favorite.path)) })
    }
}

/** What [path] holds now; a favourite whose folder or file is gone is [FavoriteKind.MISSING]. */
fun favoriteKindOf(path: String): FavoriteKind {
    val file = File(path)
    return when {
        file.isDirectory -> FavoriteKind.FOLDER
        file.exists() -> FavoriteKind.FILE
        else -> FavoriteKind.MISSING
    }
}
