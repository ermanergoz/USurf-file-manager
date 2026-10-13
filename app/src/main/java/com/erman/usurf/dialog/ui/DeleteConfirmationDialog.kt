package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.erman.usurf.R

private const val ARG_NAMES: String = "arg_names"
private const val LISTED_NAME_COUNT: Int = 3

/** Asks before the named items are deleted; Cancel is the default, and only Delete sends a result. */
class DeleteConfirmationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val names = requireArguments().getStringArrayList(ARG_NAMES).orEmpty()
        return AlertDialog.Builder(requireActivity())
            .setTitle(resources.getQuantityString(R.plurals.delete_items_title, names.size, names.size))
            .setMessage(listedNames(names))
            .setPositiveButton(R.string.delete) { _, _ -> setFragmentResult(REQUEST_KEY, bundleOf()) }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_NEGATIVE)?.requestFocus()
    }

    private fun listedNames(names: List<String>): String {
        val listed = names.take(LISTED_NAME_COUNT)
        val rest = names.size - listed.size
        val more = if (rest > 0) listOf(resources.getQuantityString(R.plurals.delete_items_more, rest, rest)) else emptyList()
        return (listed + more).joinToString(separator = "\n")
    }

    companion object {
        const val REQUEST_KEY = "delete_confirmation_request"

        fun newInstance(names: List<String>): DeleteConfirmationDialog = DeleteConfirmationDialog().apply {
            arguments = bundleOf(ARG_NAMES to ArrayList(names))
        }
    }
}
