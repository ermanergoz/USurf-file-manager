package com.erman.usurf.ftp.ui

import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/** Shows the error the id names under the field, or none for [NO_ERROR]; unchanged text is not set again. */
@BindingAdapter("errorTextRes")
fun setErrorTextRes(layout: TextInputLayout, @StringRes errorRes: Int) {
    val text: String? = if (errorRes == NO_ERROR) null else layout.context.getString(errorRes)
    if (layout.error?.toString() != text) layout.error = text
}

/** Checks the storage choice of [path] when the group lists one, each tagged with its path; a checked one is left as it is. */
@BindingAdapter("checkedPath")
fun setCheckedPath(group: RadioGroup, path: String?) {
    val choice: RadioButton = path?.let { group.findViewWithTag<RadioButton>(it) } ?: return
    if (!choice.isChecked) group.check(choice.id)
}
