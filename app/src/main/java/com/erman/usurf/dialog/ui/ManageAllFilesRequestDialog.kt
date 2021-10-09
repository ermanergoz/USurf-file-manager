package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogListener

class ManageAllFilesRequestDialog : DialogFragment() {
    private lateinit var listener: DialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.manage_all_files_message))
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                    listener.manageAllFilesRequestListener()
                }).setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { _, _ ->
                    dialog?.cancel()
                })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as DialogListener
        } catch (err: ClassCastException) {
            throw ClassCastException(("$context must implement DialogListener"))
        }
    }
}