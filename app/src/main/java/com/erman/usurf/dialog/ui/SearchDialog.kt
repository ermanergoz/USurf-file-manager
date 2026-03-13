package com.erman.usurf.dialog.ui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.OnSearchOkPressedListener

class SearchDialog : DialogFragment() {
    var onSearchOkPressedListener: OnSearchOkPressedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            this.editText = dialogView.findViewById(R.id.editText)
            builder.setTitle(getString(R.string.search))
                .setPositiveButton(R.string.ok) { _, _ ->
                    onSearchOkPressedListener?.onSearchOkPressed(editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    private lateinit var editText: EditText
}
