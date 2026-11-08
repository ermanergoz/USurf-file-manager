package com.erman.usurf.application.data

import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.storage.domain.StoragePathsProvider
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private val INITIAL_FAVORITES_LIST: List<String> =
    listOf("DCIM", "Documents", "Download", "Movies", "Music", "Pictures")

class ApplicationDao(
    private val favoriteDao: FavoriteDao,
    private val storagePathsProvider: StoragePathsProvider,
    private val ioDispatcher: CoroutineDispatcher
) {
    /** Makes the first volume's usual folders that exist favourites, on [ioDispatcher]; one stored already is kept. */
    suspend fun addInitialFavorites() = withContext(ioDispatcher) {
        val directory: String = storagePathsProvider.getStorageDirectories().firstOrNull() ?: return@withContext
        for (favoriteName in INITIAL_FAVORITES_LIST) {
            val favoritePath: String = directory + File.separator + favoriteName
            if (File(favoritePath).exists()) favoriteDao.addFavorite(favoritePath, favoriteName)
        }
    }
}
