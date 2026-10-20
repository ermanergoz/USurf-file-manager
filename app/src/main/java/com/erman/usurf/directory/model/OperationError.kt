package com.erman.usurf.directory.model

import androidx.annotation.StringRes
import com.erman.usurf.R

/**
 * Why a file operation failed, when the user can be told more than the operation's generic error. A failure the
 * operation refused before writing anything always names its error, so it is never taken for a refused write.
 */
sealed class OperationError(@StringRes val messageResId: Int) {
    /** The name asked for is already taken in that folder. */
    data object AlreadyExists : OperationError(R.string.item_already_exists)

    /** The name asked for is empty, a dot entry or holds a path separator. */
    data object InvalidName : OperationError(R.string.invalid_item_name)

    /** A rename was asked for with the name the item already has. */
    data object NameUnchanged : OperationError(R.string.name_unchanged)

    /** A folder was to be copied or moved into itself or into a folder inside it. */
    data object FolderIntoItself : OperationError(R.string.folder_into_itself)

    /** An archive name has no extension to tell its format by. */
    data object ArchiveExtensionMissing : OperationError(R.string.supported_compression_formats)

    /** A move stopped before any of its sources was deleted. */
    data object MoveFailedNothingDeleted : OperationError(R.string.move_failed_nothing_deleted)

    /** The SD card refused the write because the app holds no granted document tree for it. */
    data object SafGrantRequired : OperationError(R.string.sd_card_access_needed)

    /** 7-Zip exited 1: the archive it wrote lacked some inputs and was deleted. */
    data object SomeFilesSkipped : OperationError(R.string.some_files_could_not_be_added)

    /** 7-Zip could not write the archive, neither in its folder nor through the app's cache folder. */
    data object CompressionFailed : OperationError(R.string.error_while_compressing)

    /** 7-Zip could not extract the archive, neither into its folder nor through the app's cache folder. */
    data object ExtractionFailed : OperationError(R.string.error_while_extracting)
}

/** The message to show for this failure: its [OperationError]'s when it names one, else [fallbackResId]. */
@StringRes
fun Throwable.operationMessageOr(@StringRes fallbackResId: Int): Int =
    (this as? FileOperationException)?.error?.messageResId ?: fallbackResId
