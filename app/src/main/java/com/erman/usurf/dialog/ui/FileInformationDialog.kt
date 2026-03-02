package com.erman.usurf.dialog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogFileInformationBinding
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_EXTENSION
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_IS_DIRECTORY
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_IS_HIDDEN
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_IS_IN_ROOT
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_LAST_MODIFIED
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_NAME
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_NAME_WITHOUT_EXTENSION
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_PATH
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_SIZE
import com.erman.usurf.dialog.utils.FILE_INFORMATION_DIALOG_ARG_SUB_FILE_COUNT

class FileInformationDialog : DialogFragment() {
    private lateinit var file: FileModel
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
        } ?: error("Activity cannot be null")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_PATH, "")
        val name: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_NAME, "")
        val nameWithoutExtension: String =
            requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_NAME_WITHOUT_EXTENSION, "")
        val size: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_SIZE, "")
        val isDirectory: Boolean = requireArguments().getBoolean(FILE_INFORMATION_DIALOG_ARG_IS_DIRECTORY, false)
        val lastModified: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_LAST_MODIFIED, "")
        val extension: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_EXTENSION, "")
        val subFileCount: String = requireArguments().getString(FILE_INFORMATION_DIALOG_ARG_SUB_FILE_COUNT, "")
        val isHidden: Boolean = requireArguments().getBoolean(FILE_INFORMATION_DIALOG_ARG_IS_HIDDEN, false)
        val isInRoot: Boolean = requireArguments().getBoolean(FILE_INFORMATION_DIALOG_ARG_IS_IN_ROOT, false)
        file =
            FileModel(
                path = path,
                name = name,
                nameWithoutExtension = nameWithoutExtension,
                size = size,
                isDirectory = isDirectory,
                lastModified = lastModified,
                extension = extension,
                subFileCount = subFileCount,
                isHidden = isHidden,
                isInRoot = isInRoot,
            )
    }

    companion object {
        fun newInstance(file: FileModel): FileInformationDialog {
            val fragment: FileInformationDialog = FileInformationDialog()
            fragment.arguments =
                bundleOf(
                    FILE_INFORMATION_DIALOG_ARG_PATH to file.path,
                    FILE_INFORMATION_DIALOG_ARG_NAME to file.name,
                    FILE_INFORMATION_DIALOG_ARG_NAME_WITHOUT_EXTENSION to file.nameWithoutExtension,
                    FILE_INFORMATION_DIALOG_ARG_SIZE to file.size,
                    FILE_INFORMATION_DIALOG_ARG_IS_DIRECTORY to file.isDirectory,
                    FILE_INFORMATION_DIALOG_ARG_LAST_MODIFIED to file.lastModified,
                    FILE_INFORMATION_DIALOG_ARG_EXTENSION to file.extension,
                    FILE_INFORMATION_DIALOG_ARG_SUB_FILE_COUNT to file.subFileCount,
                    FILE_INFORMATION_DIALOG_ARG_IS_HIDDEN to file.isHidden,
                    FILE_INFORMATION_DIALOG_ARG_IS_IN_ROOT to file.isInRoot,
                )
            return fragment
        }
    }
}
