package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.ManageAllFilesRequestCallbacks

class ManageAllFilesRequestDialog : DialogFragment() {
    var callbacks: ManageAllFilesRequestCallbacks? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.manage_all_files_message))
                .setPositiveButton(R.string.ok) { _, _ ->
                    callbacks?.onManageAllFilesRequested()
                }.setNegativeButton(R.string.cancel) { _, _ ->
                    dialog?.cancel()
                }

            builder.create()
        } ?: error("Activity cannot be null")
    }

}
