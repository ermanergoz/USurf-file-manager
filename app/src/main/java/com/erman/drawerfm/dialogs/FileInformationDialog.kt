package com.erman.drawerfm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.erman.drawerfm.R
import com.erman.drawerfm.utilities.*
import java.io.File
import java.text.SimpleDateFormat

class FileInformationDialog(var file: File) : DialogFragment() {
    private lateinit var nameTextView: TextView
    private lateinit var extensionTextView: TextView
    private lateinit var pathTextView: TextView
    private lateinit var isHiddenTextView: TextView
    private lateinit var sizeTextView: TextView
    private lateinit var permissionTextView: TextView
    private lateinit var lastModificationDateTextView: TextView
    private lateinit var fileSizerogressBar: ProgressBar
    private lateinit var usedStorageProgressBar: ProgressBar
    private lateinit var fileSizePercentTextView: TextView
    private lateinit var usedStoragePercentTextView: TextView
    private val dateFormat = SimpleDateFormat("dd MMMM | HH:mm:ss")
    private var fileSizePercentage:Double = 0.0
    private var usedStoragePercentage = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_file_information, null)

            this.nameTextView = dialogView.findViewById(R.id.nameTextView)
            this.extensionTextView = dialogView.findViewById(R.id.extensionTextView)
            this.pathTextView = dialogView.findViewById(R.id.pathTextView)
            this.isHiddenTextView = dialogView.findViewById(R.id.isHiddenTextView)
            this.sizeTextView = dialogView.findViewById(R.id.sizeTextView)
            this.permissionTextView = dialogView.findViewById(R.id.permissionTextView)
            this.lastModificationDateTextView =
                dialogView.findViewById(R.id.lastModificationDateTextView)
            this.fileSizerogressBar = dialogView.findViewById(R.id.fileSizerogressBar)
            this.usedStorageProgressBar = dialogView.findViewById(R.id.usedStorageProgressBar)
            this.fileSizePercentTextView = dialogView.findViewById(R.id.fileSizePercentTextView)
            this.usedStoragePercentTextView =
                dialogView.findViewById(R.id.usedStoragePercentTextView)

            nameTextView.text = file.nameWithoutExtension

            if (file.isDirectory)
                extensionTextView.text = getString(R.string.folder)
            else
                extensionTextView.text = file.extension

            pathTextView.text = file.path
            isHiddenTextView.isVisible = file.isHidden
            isHiddenTextView.text = file.isHidden.toString()

            if (file.isDirectory) {
                this.sizeTextView.text = getConvertedFileSize(getFolderSize(file.path))
                fileSizePercentage = getFolderUsedStoragePercentage(file.path)
            } else {
                this.sizeTextView.text = getConvertedFileSize(file.length())
                fileSizePercentage =
                    getFolderUsedStoragePercentage(file.path, file.length())
            }

            if (file.canRead() && file.canWrite())
                permissionTextView.text = "-RW"
            else if (!file.canRead() && file.canWrite())
                permissionTextView.text = "-W"
            else if (file.canRead() && !file.canWrite())
                permissionTextView.text = "-R"
            else
                permissionTextView.text = "NONE"

            lastModificationDateTextView.text = dateFormat.format(file.lastModified())

            usedStoragePercentage = getUsedStoragePercentage(file.parent)
            usedStorageProgressBar.progress = usedStoragePercentage
            usedStoragePercentTextView.text = "% " + usedStoragePercentage

            fileSizerogressBar.progress = fileSizePercentage.toInt()
            fileSizePercentTextView.text = "% " + String.format("%.9f", fileSizePercentage)

            // Create the AlertDialog object and return it
            builder.setMessage(R.string.information)

                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { dialog, id ->
                    })

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}