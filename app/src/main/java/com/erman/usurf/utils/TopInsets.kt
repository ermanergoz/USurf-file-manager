package com.erman.usurf.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// What can cover the top of a screen drawn edge to edge: the status bar, and a display cutout up there.
private val TOP_BARS: Int = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

/**
 * Starts this view's content below what covers the top of the window, while its background still reaches the edge: the
 * covered height is added to the top padding the view was laid out with, again whenever it changes. The insets pass on
 * unchanged to the views inside.
 */
fun View.keepClearOfTopInset() {
    val padding: Int = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.updatePadding(top = padding + insets.getInsets(TOP_BARS).top)
        insets
    }
}
