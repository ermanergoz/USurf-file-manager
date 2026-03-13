package com.erman.drawerfm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R

class EditDialog(var title: String) : DialogFragment() {
    private lateinit var stringInput: String
    private lateinit var listener: EditDialogListener
    private lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)

            this.editText = dialogView.findViewById(R.id.editText)

            builder.setMessage(title)

                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, id ->
                    stringInput = this.editText.text.toString()
                    listener.editDialogListener(stringInput)
                }).setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, id ->
                    getDialog()?.cancel()
                })

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as EditDialogListener
        } catch (err: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement EditDialogListener"))
        }
    }

    interface EditDialogListener {
        fun editDialogListener(stringInput: String)
    }
}