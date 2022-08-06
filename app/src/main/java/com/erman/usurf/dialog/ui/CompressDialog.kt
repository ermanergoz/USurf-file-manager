package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.directory.ui.DirectoryViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel

class CompressDialog : DialogFragment() {
    private lateinit var editText: EditText
    private val editDialogViewModel by sharedViewModel<DirectoryViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)

            this.editText = dialogView.findViewById(R.id.editText)
            editText.setText(R.string.new_compressed)

            builder.setTitle(getString(R.string.compress))
                .setMessage(getString(R.string.supported_compression_formats))
                .setPositiveButton(R.string.ok) { _, _ ->
                    editDialogViewModel.onFileCompressOkPressed(editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
