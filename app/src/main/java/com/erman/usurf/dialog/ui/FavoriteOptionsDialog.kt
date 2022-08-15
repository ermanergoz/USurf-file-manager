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
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogEditFavoriteBinding
import com.erman.usurf.home.ui.HomeViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class FavoriteOptionsDialog(private val favoriteView: TextView) : DialogFragment() {
    private lateinit var okButton: Button
    private lateinit var deleteButton: Button
    private lateinit var renameEditText: EditText
    private val viewModel by viewModel<HomeViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            val binding: DialogEditFavoriteBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_edit_favorite, null, false)
            binding.setVariable(BR.favoriteView, favoriteView)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel

            this.okButton = binding.root.findViewById(R.id.okButton)
            this.renameEditText = binding.root.findViewById(R.id.renameEditText)
            this.deleteButton = binding.root.findViewById(R.id.deleteButton)

            okButton.setOnClickListener {
                viewModel.onRenameFavoriteOkPressed(favoriteView, renameEditText.text.toString())
                dismiss()
            }

            deleteButton.setOnClickListener {
                viewModel.deleteFavorites(favoriteView)
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
