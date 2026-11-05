package com.erman.usurf.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

// What can cover the bottom of a screen drawn edge to edge: the navigation bar, and a display cutout down there.
private val BOTTOM_BARS: Int = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

/**
 * Keeps this screen's content clear of what covers the bottom of the window. Each of [padded] gets the covered height
 * added to the bottom padding it was laid out with, so a list or a bar's background still reaches the edge while its
 * last row or its buttons sit above the navigation bar; each of [lifted] gets it added to its bottom margin. With
 * [withKeyboard] the keyboard counts too, for a screen with text fields. One listener, on the screen's root, applies
 * the height again whenever it changes, and passes the insets on unchanged to the views inside.
 */
fun View.keepClearOfBottomInset(padded: List<View> = emptyList(), lifted: List<View> = emptyList(), withKeyboard: Boolean = false) {
    val paddings: List<Int> = padded.map { it.paddingBottom }
    val margins: List<Int> = lifted.map { it.marginBottom }
    val types: Int = if (withKeyboard) BOTTOM_BARS or WindowInsetsCompat.Type.ime() else BOTTOM_BARS
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val covered: Int = insets.getInsets(types).bottom
        padded.forEachIndexed { index, view -> view.updatePadding(bottom = paddings[index] + covered) }
        lifted.forEachIndexed { index, view ->
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = margins[index] + covered }
        }
        insets
    }
}
