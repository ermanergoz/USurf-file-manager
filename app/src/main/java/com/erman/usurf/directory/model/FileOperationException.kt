package com.erman.usurf.directory.model

import androidx.annotation.StringRes

/** [messageResId], when set, is the message the user sees instead of the operation's general error. */
class FileOperationException(
    message: String? = null,
    val error: OperationError? = null,
    cause: Throwable? = null,
    @StringRes val messageResId: Int? = null
) : Exception(message, cause)
