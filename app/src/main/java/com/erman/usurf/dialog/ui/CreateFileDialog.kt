package com.erman.usurf.dialog.ui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.OnFileCreateOkPressedListener

class CreateFileDialog : DialogFragment() {
    var onFileCreateOkPressedListener: OnFileCreateOkPressedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            this.editText = dialogView.findViewById(R.id.editText)
            editText.setText(R.string.new_file)
            builder.setTitle(getString(R.string.create_file))
                .setPositiveButton(R.string.ok) { _, _ ->
                    onFileCreateOkPressedListener?.onFileCreateOkPressed(editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    private lateinit var editText: EditText
}
