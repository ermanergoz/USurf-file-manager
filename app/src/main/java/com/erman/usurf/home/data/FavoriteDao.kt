package com.erman.usurf.home.data

import androidx.lifecycle.LiveData

/** The stored favourites, one per path. Every write blocks its thread until the store has it: call it on io. */
interface FavoriteDao {
    /** The stored favourites, current while observed. */
    fun getFavorites(): LiveData<List<Favorite>>

    /** Stores [favoriteName] for [favoritePath]; false for an empty name, a path stored already or a refused write. */
    fun addFavorite(favoritePath: String, favoriteName: String): Boolean

    /** Removes the favourite of [favoritePath]; false when the store refused the write. */
    fun removeFavorite(favoritePath: String): Boolean

    /** Names the favourite of [favoritePath] [newName]; false when no favourite has that path or the store refused. */
    fun renameFavorite(favoritePath: String, newName: String): Boolean
}
