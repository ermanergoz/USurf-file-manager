package com.erman.usurf.dialog.ui

import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.utils.SAF_ACCESS_REQUEST_KEY

class SafAccessRequestDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.saf_access_message))
                .setPositiveButton(R.string.ok) { _, _ ->
                    parentFragmentManager.setFragmentResult(SAF_ACCESS_REQUEST_KEY, bundleOf())
                }.setNegativeButton(R.string.cancel) { _, _ ->
                    dialog?.cancel()
                }
            builder.create()
        } ?: error("Activity cannot be null")
    }
}
