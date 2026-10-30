package com.erman.usurf.utils

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Shows [dialog] on [fragmentManager] under [tag] unless a dialog under that tag is already there, one opened by an
 * earlier tap or restored after a configuration change; returns whether this one was shown. A screen shows its dialogs
 * on its own child fragment manager and listens for their results there.
 */
fun showOnce(fragmentManager: FragmentManager, dialog: DialogFragment, tag: String): Boolean {
    if (fragmentManager.findFragmentByTag(tag) != null) return false
    dialog.show(fragmentManager, tag)
    return true
}
