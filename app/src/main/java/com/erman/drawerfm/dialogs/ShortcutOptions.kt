package com.erman.drawerfm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R

class ShortcutOptions(var shortcut: TextView) : DialogFragment() {
    private lateinit var listener: ShortcutOptionListener
    private lateinit var deleteButton: Button
    private lateinit var renameButton: Button
    private lateinit var OkButton: Button
    private lateinit var renameEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_shortcut_options, null)
            var newName = ""

            this.deleteButton = dialogView.findViewById(R.id.deleteButton)
            this.renameButton = dialogView.findViewById(R.id.renameButton)
            this.OkButton = dialogView.findViewById(R.id.OkButton)
            this.renameEditText = dialogView.findViewById(R.id.renameEditText)

            renameEditText.isGone = true
            OkButton.isGone = true

            builder.setMessage("").setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, id ->
                getDialog()?.cancel()
            })
            deleteButton.setOnClickListener {
                listener.shortcutOptionListener(isDelete = true,
                                                isRename = false,
                                                shortcutView = shortcut,
                                                newName = "")
                dialog?.cancel()
            }

            renameButton.setOnClickListener {
                deleteButton.isGone = true
                renameButton.isGone = true
                renameEditText.isGone = false
                OkButton.isGone = false
            }

            OkButton.setOnClickListener {
                newName = this.renameEditText.text.toString()

                listener.shortcutOptionListener(isDelete = false,
                                                isRename = true,
                                                shortcutView = shortcut,
                                                newName = newName)
                dialog?.cancel()
            }

            // Create the AlertDialog object and return it
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ShortcutOptionListener
        } catch (err: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement ShortcutOptionListener"))
        }
    }

    interface ShortcutOptionListener {
        fun shortcutOptionListener(isDelete: Boolean, isRename: Boolean, shortcutView: TextView, newName: String)
    }
}