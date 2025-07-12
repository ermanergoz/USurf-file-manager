package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.DialogListener

class SafAccessRequestDialog : DialogFragment() {
    private lateinit var listener: DialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.saf_access_message))
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener.safAccessRequestListener()
                }.setNegativeButton(R.string.cancel) { _, _ ->
                    dialog?.cancel()
                }

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
