package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogEditShortcutBinding
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.utils.ViewModelFactory
import kotlinx.android.synthetic.main.dialog_edit_shortcut.*

class ShortcutOptionsDialog(private val shortcutView: TextView) : DialogFragment() {
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var viewModel: HomeViewModel
    private lateinit var okButton: Button
    private lateinit var deleteButton: Button
    private lateinit var renameEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            viewModelFactory = ViewModelFactory()
            viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(HomeViewModel::class.java)

            val binding: DialogEditShortcutBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_edit_shortcut, null, false)
            binding.setVariable(BR.shortcutView, shortcutView)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel

            this.okButton = binding.root.findViewById(R.id.okButton)
            this.renameEditText = binding.root.findViewById(R.id.renameEditText)
            this.deleteButton = binding.root.findViewById(R.id.deleteButton)

            okButton.setOnClickListener {
                viewModel.onRenameShortcutOkPressed(shortcutView, renameEditText.text.toString())
                dismiss()
            }

            deleteButton.setOnClickListener {
                viewModel.deleteShortcut(shortcutView)
                dismiss()
            }

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.turnOffRenameMode()
        super.onDismiss(dialog)
    }
}
