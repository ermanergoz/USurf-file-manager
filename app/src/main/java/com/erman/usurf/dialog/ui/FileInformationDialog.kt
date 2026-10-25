package com.erman.usurf.dialog.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.erman.usurf.R
import com.erman.usurf.databinding.DialogFileInformationBinding
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.directory.model.MountOption
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.directory.ui.forInformation
import org.koin.androidx.viewmodel.ext.android.activityViewModel

private const val ARG_FILES: String = "arg_files"

/** The information of one or more items, one page each, titled by the name of the item on the page. */
class FileInformationDialog : DialogFragment() {
    private val directoryViewModel: DirectoryViewModel by activityViewModel()

    // A folder's size is walked once per dialog, however often its page is shown again.
    private val folderSizes: MutableMap<String, LiveData<String>> = mutableMapOf()

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
        val shownFile = file.forInformation(getString(R.string.not_available_in_root_mode))
        binding.permissionText = getString(file.permissionLabel())
        binding.file = shownFile
        // A folder's size takes a walk of everything in it, so it is read only now that its Information is open.
        if (file.isDirectory && !file.isInRoot) {
            binding.file = shownFile.copy(size = getString(R.string.calculating_size))
            folderSizes.getOrPut(file.path) { directoryViewModel.folderSizeOf(file.path) }.observe(this) { size ->
                binding.file = shownFile.copy(size = size)
            }
        }
    }

    private inner class InformationPages(private val files: List<FileModel>) : PagerAdapter() {
        override fun getCount(): Int = files.size

        override fun isViewFromObject(view: View, page: Any): Boolean = view === page

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = DialogFileInformationBinding.inflate(layoutInflater, container, false)
            bind(binding, files[position])
            binding.executePendingBindings()
            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, page: Any) {
            container.removeView(page as View)
        }
    }

    companion object {
        fun newInstance(files: List<FileModel>): FileInformationDialog = FileInformationDialog().apply {
            arguments = bundleOf(ARG_FILES to ArrayList(files))
        }
    }
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
