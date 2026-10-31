package com.erman.usurf.home.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.FavoriteKind
import com.erman.usurf.storage.domain.StoragePathsProvider
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** The favourites of [favoriteDao], each written on [ioDispatcher], never on the main thread. */
class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val storagePathsProvider: StoragePathsProvider,
    private val ioDispatcher: CoroutineDispatcher
) : FavoriteRepository {
    override fun getFavorites(): LiveData<List<FavoriteItem>> =
        favoritesWithKinds(favoriteDao.getFavorites(), ioDispatcher, ::favoriteKindOf)

    override suspend fun addFavorite(path: String, name: String): Boolean = withContext(ioDispatcher) {
        favoriteDao.addFavorite(path, name)
    }

    override suspend fun removeFavorite(path: String): Boolean = withContext(ioDispatcher) { favoriteDao.removeFavorite(path) }

    override suspend fun renameFavorite(path: String, newName: String): Boolean = withContext(ioDispatcher) {
        favoriteDao.renameFavorite(path, newName)
    }

    override fun defaultNameFor(path: String): String? = defaultFavoriteNameOf(path, storagePathsProvider.getStorageDirectories())
}

/**
 * The name a new favourite of the folder at [path] is offered: its own. A storage directory of [storageDirectories] is
 * named by its volume's id, as "0" or "1A2B-3C4D", and the file system root has no name, so neither gets one.
 */
fun defaultFavoriteNameOf(path: String, storageDirectories: Set<String>): String? =
    if (path in storageDirectories) null else File(path).name.ifEmpty { null }

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
