package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.erman.usurf.R
import java.io.File

private const val ARG_PATHS: String = "arg_paths"
private const val LISTED_NAME_COUNT: Int = 3

/**
 * Asks before the items at the given paths are deleted; Cancel is the default, and only Delete sends a result.
 * The paths travel in the arguments and come back in the result, so a dialog the system restores after the
 * process was killed still confirms exactly the items it shows.
 */
class DeleteConfirmationDialog : DialogFragment() {
    private val targetPaths: List<String>
        get() = requireArguments().getStringArrayList(ARG_PATHS).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val names = targetPaths.map { File(it).name }
        return AlertDialog.Builder(requireActivity())
            .setTitle(resources.getQuantityString(R.plurals.delete_items_title, names.size, names.size))
            .setMessage(listedNames(names))
            .setPositiveButton(R.string.delete) { _, _ -> setFragmentResult(REQUEST_KEY, confirmationResult()) }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_NEGATIVE)?.requestFocus()
    }

    /** The result Delete sends: the paths this dialog asks about. */
    internal fun confirmationResult(): Bundle = bundleOf(RESULT_PATHS to ArrayList(targetPaths))

    private fun listedNames(names: List<String>): String {
        val listed = names.take(LISTED_NAME_COUNT)
        val rest = names.size - listed.size
        val more = if (rest > 0) listOf(resources.getQuantityString(R.plurals.delete_items_more, rest, rest)) else emptyList()
        return (listed + more).joinToString(separator = "\n")
    }

    companion object {
        const val TAG = "delete_confirmation"
        const val REQUEST_KEY = "delete_confirmation_request"
        const val RESULT_PATHS = "result_paths"

        fun newInstance(paths: List<String>): DeleteConfirmationDialog = DeleteConfirmationDialog().apply {
            arguments = bundleOf(ARG_PATHS to ArrayList(paths))
        }

        /** The paths a result of this dialog confirms for deletion; none when it names none. */
        fun pathsIn(result: Bundle): List<String> = result.getStringArrayList(RESULT_PATHS).orEmpty()
    }
}
