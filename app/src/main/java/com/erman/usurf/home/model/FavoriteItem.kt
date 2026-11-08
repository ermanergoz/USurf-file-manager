package com.erman.usurf.home.model

/** What a favourite's path holds now, read when the favourites are listed, so a tap on one needs no file system call. */
enum class FavoriteKind { FOLDER, FILE, MISSING }

data class FavoriteItem(val id: String, val name: String, val path: String, val kind: FavoriteKind) {
    val isDirectory: Boolean
        get() = kind == FavoriteKind.FOLDER
}
