package com.erman.usurf.home.model

interface HomeStorageButton {
    fun autoSizeButtonDimensions(storageButtonCount: Int, sideMargin: Int): Pair<Int, Int>
}