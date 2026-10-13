package com.erman.usurf.directory.model

import androidx.annotation.StringRes
import com.erman.usurf.R

/** Why a file operation failed, when the user can be told more than the operation's generic error. */
sealed class OperationError(@StringRes val messageResId: Int) {
    /** The name asked for is already taken in that folder. */
    data object AlreadyExists : OperationError(R.string.item_already_exists)
}

/** The message to show for this failure: its [OperationError]'s when it names one, else [fallbackResId]. */
@StringRes
fun Throwable.operationMessageOr(@StringRes fallbackResId: Int): Int =
    (this as? FileOperationException)?.error?.messageResId ?: fallbackResId
