package com.erman.usurf.home.ui

import com.erman.usurf.home.model.FavoriteItem

interface FavoriteItemListener {
    fun onFavoriteClick(item: FavoriteItem)

    fun onFavoriteLongClick(path: String, name: String): Boolean
}
