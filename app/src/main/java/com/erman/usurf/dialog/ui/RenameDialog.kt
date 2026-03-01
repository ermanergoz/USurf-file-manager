package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.OnRenameOkPressedListener
import com.erman.usurf.dialog.utils.RENAME_DIALOG_ARG_NAME

class RenameDialog : DialogFragment() {
    var onRenameOkPressedListener: OnRenameOkPressedListener? = null
    private var name: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            this.editText = dialogView.findViewById(R.id.editText)
            name?.let { n -> editText.setText(n) } ?: editText.setText("")
            builder.setTitle(getString(R.string.rename))
                .setPositiveButton(R.string.ok) { _, _ ->
                    onRenameOkPressedListener?.onRenameOkPressed(editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        name = arguments?.getString(RENAME_DIALOG_ARG_NAME)
    }

    private lateinit var editText: EditText

    companion object {
        fun newInstance(name: String?): RenameDialog {
            val fragment: RenameDialog = RenameDialog()
            fragment.arguments = bundleOf(RENAME_DIALOG_ARG_NAME to name)
            return fragment
        }
    }
}
