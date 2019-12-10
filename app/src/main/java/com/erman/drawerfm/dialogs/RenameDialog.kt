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

class RenameDialog : DialogFragment() {
    private lateinit var newFileName: String
    private lateinit var listener: DialogRenameFileListener
    private lateinit var nameEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.rename_file_dialog, null)

            this.nameEditText = dialogView.findViewById(R.id.nameEditText)

            // Create the AlertDialog object and return it
            builder.setMessage(R.string.rename_file)

                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { dialog, id ->
                        // Send the positive button event back to the host activity
                        newFileName = this.nameEditText.text.toString()
                        listener.dialogRenameFileListener(newFileName)
                    })
                .setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as DialogRenameFileListener
        } catch (err: ClassCastException) {
            throw ClassCastException(
                (context.toString() + " must implement DialogRenameFileListener")
            )
        }
    }

    interface DialogRenameFileListener {
        fun dialogRenameFileListener(newFileName: String)
    }
}