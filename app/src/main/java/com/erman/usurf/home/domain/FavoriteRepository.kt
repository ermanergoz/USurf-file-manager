package com.erman.usurf.home.domain

import androidx.lifecycle.LiveData
import com.erman.usurf.home.model.FavoriteItem

interface FavoriteRepository {
    fun getFavorites(): LiveData<List<FavoriteItem>>

    fun addFavorite(
        path: String,
        name: String,
    ): Boolean

    fun removeFavorite(path: String): Boolean

    fun renameFavorite(
        path: String,
        newName: String,
    ): Boolean
}
