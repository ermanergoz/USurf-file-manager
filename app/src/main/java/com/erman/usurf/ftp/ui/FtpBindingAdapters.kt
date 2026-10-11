package com.erman.usurf.ftp.ui

import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/** Shows the error the id names under the field, or none for [NO_ERROR]; unchanged text is not set again. */
@BindingAdapter("errorTextRes")
fun setErrorTextRes(layout: TextInputLayout, @StringRes errorRes: Int) {
    val text: String? = if (errorRes == NO_ERROR) null else layout.context.getString(errorRes)
    if (layout.error?.toString() != text) layout.error = text
}
