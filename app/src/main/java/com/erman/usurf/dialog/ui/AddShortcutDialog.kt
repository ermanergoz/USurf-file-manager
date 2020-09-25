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
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.utils.ViewModelFactory

class AddShortcutDialog(val path: String) : DialogFragment() {
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            viewModelFactory = ViewModelFactory()
            homeViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(HomeViewModel::class.java)
            directoryViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DirectoryViewModel::class.java)

            this.editText = dialogView.findViewById(R.id.editText)

            directoryViewModel.turnOffOptionPanel()
            directoryViewModel.clearMultipleSelection()

            builder.setMessage(getString(R.string.addShortcut))
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                    homeViewModel.onShortcutAdd(path, editText.text.toString())

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
