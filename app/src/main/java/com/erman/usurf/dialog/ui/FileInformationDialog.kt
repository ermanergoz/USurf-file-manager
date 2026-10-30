package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogFileInformationBinding
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.directory.model.MountOption
import com.erman.usurf.utils.SizeFormatter
import com.erman.usurf.utils.modificationDateText
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val ARG_FILES: String = "arg_files"

/** The information of one or more items, one page each, titled by the name of the item on the page. */
class FileInformationDialog : DialogFragment() {
    // This dialog's own view model, kept while the dialog is made again, so a folder's size is walked once per dialog.
    private val informationViewModel: FileInformationViewModel by viewModel()

    // The pages on screen, by the path of the item each shows, and the folder sizes read so far: a page shown again
    // binds the size it has, and each folder's size is observed once, not once per showing of its page.
    private val pagesByPath: MutableMap<String, DialogFileInformationBinding> = mutableMapOf()
    private val folderSizeTexts: MutableMap<String, String> = mutableMapOf()

    private val files: List<FileModel>
        get() = BundleCompat.getParcelableArrayList(requireArguments(), ARG_FILES, FileModel::class.java).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val files = files
        val pages = WrapContentViewPager(requireContext()).apply { id = R.id.informationPages }
        pages.adapter = InformationPages(files)
        val dialog =
            AlertDialog.Builder(requireActivity())
                .setTitle(titleOf(files.firstOrNull()))
                .setView(pages)
                .setPositiveButton(R.string.ok, null)
                .create()
        pages.addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    dialog.setTitle(titleOf(files.getOrNull(position)))
                }
            }
        )
        return dialog
    }

    private fun titleOf(file: FileModel?): String = getString(R.string.information_title, file?.name.orEmpty())

    private fun bind(binding: DialogFileInformationBinding, file: FileModel) {
        binding.file = file
        binding.permissionText = getString(file.permissionLabel())
        binding.dateText = resources.informationDateText(file)
        binding.sizeText = folderSizeTexts[file.path] ?: resources.informationSizeText(file)
        // A folder's size takes a walk of everything in it, so it is read only once its page is first shown.
        if (file.isDirectory && !file.isInRoot && file.path !in folderSizeTexts) observeFolderSize(file.path)
    }

    private fun observeFolderSize(path: String) {
        folderSizeTexts[path] = getString(R.string.calculating_size)
        informationViewModel.folderSize(path).observe(this) { size ->
            val text = SizeFormatter.fileSize(resources, size)
            folderSizeTexts[path] = text
            pagesByPath[path]?.sizeText = text
        }
    }

    private inner class InformationPages(private val files: List<FileModel>) : PagerAdapter() {
        override fun getCount(): Int = files.size

        override fun isViewFromObject(view: View, page: Any): Boolean = view === page

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = DialogFileInformationBinding.inflate(layoutInflater, container, false)
            pagesByPath[files[position].path] = binding
            bind(binding, files[position])
            binding.executePendingBindings()
            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, page: Any) {
            pagesByPath.remove(files[position].path)
            container.removeView(page as View)
        }
    }

    companion object {
        const val TAG = "file_information"

        fun newInstance(files: List<FileModel>): FileInformationDialog = FileInformationDialog().apply {
            arguments = bundleOf(ARG_FILES to ArrayList(files))
        }
    }
}

/**
 * What the Size row says for this item: its size; for a folder, that it is being read until its walk ends; for an
 * entry listed through root, that it is not known.
 */
internal fun Resources.informationSizeText(file: FileModel): String = when {
    file.isInRoot -> getString(R.string.not_available_in_root_mode)
    file.isDirectory -> getString(R.string.calculating_size)
    else -> SizeFormatter.fileSize(this, file.size ?: 0L)
}

/** What the last-change row says for this item; an entry listed through root has no date read, and says so. */
internal fun Resources.informationDateText(file: FileModel): String = if (file.isInRoot) {
    getString(R.string.not_available_in_root_mode)
} else {
    file.lastModified?.let { millis -> modificationDateText(millis) }.orEmpty()
}

/** What the Permissions row says for this item: what the app may do with it, or that root listings do not tell. */
@StringRes
internal fun FileModel.permissionLabel(): Int = if (isInRoot) {
    R.string.not_available_in_root_mode
} else {
    when (permission) {
        MountOption.READ_WRITE -> R.string.permission_read_write
        MountOption.READ -> R.string.permission_read_only
        MountOption.OTHER -> R.string.permission_none
    }
}
