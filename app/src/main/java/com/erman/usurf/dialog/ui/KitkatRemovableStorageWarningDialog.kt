package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R

class KitkatRemovableStorageWarningDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setMessage(getString(R.string.kitkat_removable_storage_warning))
                .setPositiveButton(R.string.i_understand, DialogInterface.OnClickListener { _, _ ->
                    dismiss()
                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
