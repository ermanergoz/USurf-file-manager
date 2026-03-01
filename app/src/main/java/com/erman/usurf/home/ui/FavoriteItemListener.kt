package com.erman.usurf.home.ui

interface FavoriteItemListener {
    fun onFavoriteClick(path: String)

    fun onFavoriteLongClick(
        path: String,
        name: String,
    ): Boolean
}
