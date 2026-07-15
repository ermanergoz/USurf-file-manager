package com.erman.usurf.dialog.ui

import android.app.Dialog
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

    private lateinit var editText: EditText

    companion object {
        const val REQUEST_KEY = "compress_request"
        const val RESULT_NAME = "result_name"
    }
}
