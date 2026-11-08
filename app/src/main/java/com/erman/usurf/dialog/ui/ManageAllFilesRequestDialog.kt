package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.utils.MANAGE_ALL_FILES_REQUEST_KEY
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ManageAllFilesRequestDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let {
        val builder = MaterialAlertDialogBuilder(it)
        builder.setMessage(getString(R.string.manage_all_files_message))
            .setPositiveButton(R.string.open_settings) { _, _ ->
                parentFragmentManager.setFragmentResult(MANAGE_ALL_FILES_REQUEST_KEY, bundleOf())
            }.setNegativeButton(R.string.cancel) { _, _ ->
                dialog?.cancel()
            }
        builder.create()
    } ?: error("Activity cannot be null")
}
