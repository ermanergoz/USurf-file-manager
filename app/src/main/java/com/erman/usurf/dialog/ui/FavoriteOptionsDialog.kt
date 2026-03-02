package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogEditFavoriteBinding
import com.erman.usurf.dialog.model.FavoriteOptionsDialogListener
import com.erman.usurf.dialog.utils.FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_NAME
import com.erman.usurf.dialog.utils.FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_PATH

class FavoriteOptionsDialog : DialogFragment() {
    private lateinit var favoritePath: String
    private lateinit var favoriteName: String
    var listener: FavoriteOptionsDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val binding: DialogEditFavoriteBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_edit_favorite, null, false)
            binding.favoriteName = favoriteName
            binding.lifecycleOwner = this
            binding.uiState = listener?.getUiState()?.value ?: binding.uiState
            listener?.getUiState()?.observe(
                this,
                Observer { state ->
                    binding.uiState = state
                },
            )
            val okButton = binding.root.findViewById<Button>(R.id.okButton)
            val renameButton = binding.root.findViewById<Button>(R.id.renameButton)
            val renameEditText = binding.root.findViewById<EditText>(R.id.renameEditText)
            val deleteButton = binding.root.findViewById<Button>(R.id.deleteButton)
            renameEditText.setText(favoriteName)
            renameButton.setOnClickListener {
                dismiss()
                listener?.onRenameButtonClick(favoritePath, favoriteName)
            }
            okButton.setOnClickListener {
                listener?.onRename(favoritePath, renameEditText.text.toString())
                dismiss()
            }
            deleteButton.setOnClickListener {
                listener?.onDelete(favoritePath)
                dismiss()
            }
            builder.setView(binding.root)
            builder.create()
        } ?: error("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener?.onDismiss()
        super.onDismiss(dialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritePath = requireArguments().getString(FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_PATH, "")
        favoriteName = requireArguments().getString(FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_NAME, "")
    }

    companion object {
        fun newInstance(
            favoritePath: String,
            favoriteName: String,
        ): FavoriteOptionsDialog {
            val fragment: FavoriteOptionsDialog = FavoriteOptionsDialog()
            fragment.arguments =
                bundleOf(
                    FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_PATH to favoritePath,
                    FAVORITE_OPTIONS_DIALOG_ARG_FAVORITE_NAME to favoriteName,
                )
            return fragment
        }
    }
}
