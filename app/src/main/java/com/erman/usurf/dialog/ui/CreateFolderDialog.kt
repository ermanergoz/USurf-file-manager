package com.erman.usurf.dialog.ui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.OnFolderCreateOkPressedListener

class CreateFolderDialog : DialogFragment() {
    var onFolderCreateOkPressedListener: OnFolderCreateOkPressedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            this.editText = dialogView.findViewById(R.id.editText)
            editText.setText(R.string.new_folder)
            builder.setTitle(getString(R.string.create_new_folder))
                .setPositiveButton(R.string.ok) { _, _ ->
                    onFolderCreateOkPressedListener?.onFolderCreateOkPressed(editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    private lateinit var editText: EditText
}
