package com.erman.usurf.directory.ui

import android.content.res.Resources
import com.erman.usurf.R

/** What a button that opens a menu reads to a screen reader: whether its menu is open. */
fun Resources.expandedStateText(isExpanded: Boolean): String =
    getString(if (isExpanded) R.string.state_expanded else R.string.state_collapsed)
