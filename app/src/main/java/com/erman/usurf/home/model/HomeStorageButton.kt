package com.erman.usurf.home.model

interface HomeStorageButton {
    fun autoSizeButtonDimensions(
        storageButtonCount: Int,
        sideMargin: Int,
        containerHorizontalPadding: Int,
    ): Pair<Int, Int>
}
