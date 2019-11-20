package com.erman.drawerfm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R

class ConfirmationDialog(var message: String) : DialogFragment() {

    private lateinit var listener: DialogConfirmationListener
    var isPositiveButtonClicked = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)
            builder.setMessage(message)
                .setPositiveButton(R.string.yes,
                    DialogInterface.OnClickListener { dialog, id ->
                        listener.dialogConfirmationInfo(isPositiveButtonClicked)
                    })
                .setNegativeButton(R.string.no,
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as DialogConfirmationListener
        } catch (err: ClassCastException) {
            throw ClassCastException(
                (context.toString() + " must implement ConfirmationDialogListener")
            )
        }
    }

    interface DialogConfirmationListener {
        fun dialogConfirmationInfo(choice: Boolean)
    }
}