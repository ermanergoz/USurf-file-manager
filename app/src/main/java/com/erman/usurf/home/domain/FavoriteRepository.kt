package com.erman.usurf.home.domain

import androidx.lifecycle.LiveData
import com.erman.usurf.home.model.FavoriteItem

interface FavoriteRepository {
    fun getFavorites(): LiveData<List<FavoriteItem>>

    suspend fun addFavorite(path: String, name: String): Boolean

    suspend fun removeFavorite(path: String): Boolean

    suspend fun renameFavorite(path: String, newName: String): Boolean

    /** The name a new favourite of the folder at [path] is offered: the folder's own; none for a volume root, named by the screen. */
    fun defaultNameFor(path: String): String?
}
