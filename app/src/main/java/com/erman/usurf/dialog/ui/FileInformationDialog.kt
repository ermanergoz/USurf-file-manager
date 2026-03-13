package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.DialogFragment
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogFileInformationBinding
import com.erman.usurf.directory.model.FileModel

class FileInformationDialog(var file: FileModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            val binding: DialogFileInformationBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_file_information, null, false)
            binding.setVariable(BR.file, file)
            binding.lifecycleOwner = this

            builder.setTitle(file.name + " " + getString(R.string.information))
                .setPositiveButton(R.string.ok) { _, _ ->
                }

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}