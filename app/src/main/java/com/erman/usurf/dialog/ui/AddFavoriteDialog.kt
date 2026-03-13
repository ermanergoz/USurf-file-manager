package com.erman.usurf.dialog.ui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.dialog.model.AddFavoriteDialogCallbacks

private const val ADD_FAVORITE_DIALOG_ARG_PATH: String = "arg_path"

class AddFavoriteDialog : DialogFragment() {
    var callbacks: AddFavoriteDialogCallbacks? = null
    private lateinit var path: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            callbacks?.onDialogShown()
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
            this.editText = dialogView.findViewById(R.id.editText)
            editText.setText(R.string.new_favorite)
            builder.setTitle(getString(R.string.addFavorite))
                .setPositiveButton(R.string.ok) { _, _ ->
                    callbacks?.onAddFavorite(path, editText.text.toString())
                }
            builder.setView(dialogView)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = requireArguments().getString(ADD_FAVORITE_DIALOG_ARG_PATH, "")
    }

    companion object {
        fun newInstance(path: String): AddFavoriteDialog {
            val fragment = AddFavoriteDialog()
            fragment.arguments = bundleOf(ADD_FAVORITE_DIALOG_ARG_PATH to path)
            return fragment
        }
    }
}
