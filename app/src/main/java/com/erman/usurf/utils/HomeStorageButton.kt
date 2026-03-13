package com.erman.usurf.utils

interface HomeStorageButton {
    fun autoSizeButtonDimensions(storageButtonCount: Int, sideMargin: Int): Pair<Int, Int>
}