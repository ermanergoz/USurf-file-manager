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

private const val ADD_FAVORITE_DIALOG_ARG_PATH: String = "arg_path"

class AddFavoriteDialog : DialogFragment() {
    private lateinit var path: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let {
        val builder = AlertDialog.Builder(it)
        val inflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
        this.editText = dialogView.findViewById(R.id.editText)
        editText.setText(R.string.new_favorite)
        builder.setTitle(getString(R.string.addFavorite))
            .setPositiveButton(R.string.ok) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_PATH to path, RESULT_NAME to editText.text.toString()))
            }
        builder.setView(dialogView)
        builder.create()
    } ?: error("Activity cannot be null")

    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = requireArguments().getString(ADD_FAVORITE_DIALOG_ARG_PATH, "")
    }

    companion object {
        const val REQUEST_KEY = "add_favorite_request"
        const val RESULT_PATH = "result_path"
        const val RESULT_NAME = "result_name"

        fun newInstance(path: String): AddFavoriteDialog {
            val fragment = AddFavoriteDialog()
            fragment.arguments = bundleOf(ADD_FAVORITE_DIALOG_ARG_PATH to path)
            return fragment
        }
    }
}
