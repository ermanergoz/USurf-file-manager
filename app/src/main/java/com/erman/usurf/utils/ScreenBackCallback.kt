package com.erman.usurf.utils

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher

/**
 * Lets a screen decide Back before the activity does: [handle] returns whether the screen used it. When it did not,
 * Back goes on to the next handler of [dispatcher], the activity's, as if this one were not there.
 */
class ScreenBackCallback(private val dispatcher: OnBackPressedDispatcher, private val handle: () -> Boolean) :
    OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (handle()) return
        isEnabled = false
        try {
            dispatcher.onBackPressed()
        } finally {
            isEnabled = true
        }
    }
}
