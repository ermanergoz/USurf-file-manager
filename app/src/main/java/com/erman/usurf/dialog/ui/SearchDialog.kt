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

class SearchDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let {
        val builder = AlertDialog.Builder(it)
        val inflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_edit, null)
        this.editText = dialogView.findViewById(R.id.editText)
        builder.setTitle(getString(R.string.search))
            .setPositiveButton(R.string.ok) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_QUERY to editText.text.toString()))
            }
        builder.setView(dialogView)
        builder.create()
    } ?: error("Activity cannot be null")

    private lateinit var editText: EditText

    companion object {
        const val REQUEST_KEY = "search_request"
        const val RESULT_QUERY = "result_query"
    }
}
