package com.erman.usurf.directory.ui

import android.content.res.Resources
import com.erman.usurf.R

/**
 * What a row's state reads to a screen reader: while items are selected, whether the row is one of them; while none is,
 * nothing, as a tap then opens the row instead of selecting it.
 */
fun Resources.selectionStateText(isSelected: Boolean, isSelecting: Boolean): String? = when {
    !isSelecting -> null
    isSelected -> getString(R.string.state_selected)
    else -> getString(R.string.state_not_selected)
}

/** The app bar's title while items are selected: how many there are. */
fun Resources.selectionTitle(count: Int): String = getQuantityString(R.plurals.items_selected, count, count)

/** What a button that opens a menu reads to a screen reader: whether its menu is open. */
fun Resources.expandedStateText(isExpanded: Boolean): String =
    getString(if (isExpanded) R.string.state_expanded else R.string.state_collapsed)
