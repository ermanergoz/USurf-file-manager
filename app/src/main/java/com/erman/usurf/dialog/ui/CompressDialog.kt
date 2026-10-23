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

class CompressDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let {
        val builder = AlertDialog.Builder(it)
        val inflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
        this.editText = dialogView.findViewById(R.id.editText)
        editText.setText(R.string.new_compressed)
        builder.setTitle(getString(R.string.compress))
            .setMessage(getString(R.string.supported_compression_formats))
            .setPositiveButton(R.string.ok) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_NAME to editText.text.toString()))
            }
        builder.setView(dialogView)
        builder.create()
    } ?: error("Activity cannot be null")

    // A cancel tells the screen that the items this dialog was opened for are no longer waiting for an answer.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setFragmentResult(CANCEL_REQUEST_KEY, Bundle())
    }

    private lateinit var editText: EditText

    companion object {
        const val REQUEST_KEY = "compress_request"
        const val CANCEL_REQUEST_KEY = "compress_cancel_request"
        const val RESULT_NAME = "result_name"
    }
}
