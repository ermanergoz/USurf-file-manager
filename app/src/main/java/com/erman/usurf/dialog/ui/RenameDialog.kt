package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.R
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.utils.ViewModelFactory

class RenameDialog(val name: String?) : DialogFragment() {
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var editDialogViewModel: DirectoryViewModel
    private lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            viewModelFactory = ViewModelFactory()
            editDialogViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DirectoryViewModel::class.java)

            this.editText = dialogView.findViewById(R.id.editText)
            name?.let { editText.setText(it) } ?: let { editText.setText("") }

            builder.setMessage(getString(R.string.rename))
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                    editDialogViewModel.onRenameOkPressed(editText.text.toString())

                    val inputMethodManager: InputMethodManager =
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    if (inputMethodManager.isActive)
                        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
                })
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
