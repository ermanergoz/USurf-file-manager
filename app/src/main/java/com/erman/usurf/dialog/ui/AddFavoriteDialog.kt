package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.ui.HomeViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class AddFavoriteDialog(val path: String) : DialogFragment() {
    private lateinit var editText: EditText
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()
    private val homeViewModel by viewModel<HomeViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)

            this.editText = dialogView.findViewById(R.id.editText)

            directoryViewModel.turnOffOptionPanel()
            directoryViewModel.clearMultipleSelection()
            editText.setText(R.string.new_favorite)

            builder.setTitle(getString(R.string.addFavorite))
                .setPositiveButton(R.string.ok) { _, _ ->
                    homeViewModel.onFavoriteAdd(path, editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
