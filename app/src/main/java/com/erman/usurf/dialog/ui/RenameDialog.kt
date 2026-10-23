package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.erman.usurf.R
import com.erman.usurf.dialog.model.OnRenameOkPressedListener

private const val RENAME_DIALOG_ARG_NAME: String = "arg_name"

class RenameDialog : DialogFragment() {
    var onRenameOkPressedListener: OnRenameOkPressedListener? = null
    private var name: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        name = arguments?.getString(RENAME_DIALOG_ARG_NAME)
    }

    // A cancel tells the screen that the items this dialog was opened for are no longer waiting for an answer.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setFragmentResult(CANCEL_REQUEST_KEY, Bundle())
    }

    private lateinit var editText: EditText

    companion object {
        const val CANCEL_REQUEST_KEY = "rename_cancel_request"

        fun newInstance(name: String?): RenameDialog {
            val fragment = RenameDialog()
            fragment.arguments = bundleOf(RENAME_DIALOG_ARG_NAME to name)
            return fragment
        }
    }
}
