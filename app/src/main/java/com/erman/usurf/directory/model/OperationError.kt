package com.erman.usurf.directory.model

import androidx.annotation.StringRes
import com.erman.usurf.R

/** Why a file operation failed, when the user can be told more than the operation's generic error. */
sealed class OperationError(@StringRes val messageResId: Int) {
    /** The name asked for is already taken in that folder. */
    data object AlreadyExists : OperationError(R.string.item_already_exists)

    /** A move stopped before any of its sources was deleted. */
    data object MoveFailedNothingDeleted : OperationError(R.string.move_failed_nothing_deleted)

    /** The SD card refused the write because the app holds no granted document tree for it. */
    data object SafGrantRequired : OperationError(R.string.sd_card_access_needed)
}

/** The message to show for this failure: its [OperationError]'s when it names one, else [fallbackResId]. */
@StringRes
fun Throwable.operationMessageOr(@StringRes fallbackResId: Int): Int =
    (this as? FileOperationException)?.error?.messageResId ?: fallbackResId
