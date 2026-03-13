package com.erman.drawerfm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R


class AboutDrawerFMDialog : DialogFragment() {
    private lateinit var linkText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.about_drawerfm_dialog, null)
            val builder = AlertDialog.Builder(it)

            this.linkText = dialogView.findViewById(R.id.linkText)

            var link = "https://github.com/ermanergoz/DraverFM"
            linkText.text = link
            linkText.isSingleLine = true

            builder.setMessage(R.string.about)

                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, id ->
                    getDialog()?.cancel()
                })

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}