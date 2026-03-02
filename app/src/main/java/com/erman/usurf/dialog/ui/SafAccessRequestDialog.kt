package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.SafAccessRequestCallbacks

class SafAccessRequestDialog : DialogFragment() {
    var callbacks: SafAccessRequestCallbacks? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.saf_access_message))
                .setPositiveButton(R.string.ok) { _, _ ->
                    callbacks?.onSafAccessRequested()
                }.setNegativeButton(R.string.cancel) { _, _ ->
                    dialog?.cancel()
                }

            builder.create()
        } ?: error("Activity cannot be null")
    }

}
