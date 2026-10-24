package com.erman.usurf.dialog.ui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * What a text input dialog asks for: its title, the field's label and the text it opens with, an optional message
 * above the field, the positive button's label, and whether the text must be a valid item name. The result is sent
 * under [requestKey] and carries [payload] back with the text, so a dialog restored after the process was stopped
 * still answers for what it was opened for. [selectedLength] characters of the default text are selected when the
 * dialog opens; null selects all of it.
 */
@Parcelize
data class TextInputRequest(
    val requestKey: String,
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val defaultText: String = "",
    @StringRes val message: Int = 0,
    @StringRes val positiveButton: Int,
    val validateAsName: Boolean,
    val payload: Bundle = Bundle.EMPTY,
    val selectedLength: Int? = null
) : Parcelable
