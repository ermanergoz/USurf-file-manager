package com.erman.usurf.directory.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentDirectoryBinding
import com.erman.usurf.dialog.model.AddFavoriteDialogCallbacks
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.model.OnCompressOkPressedListener
import com.erman.usurf.dialog.model.OnFileCreateOkPressedListener
import com.erman.usurf.dialog.model.OnFolderCreateOkPressedListener
import com.erman.usurf.dialog.model.OnRenameOkPressedListener
import com.erman.usurf.dialog.model.OnSearchOkPressedListener
import com.erman.usurf.dialog.ui.AddFavoriteDialog
import com.erman.usurf.dialog.ui.CompressDialog
import com.erman.usurf.dialog.ui.CreateFileDialog
import com.erman.usurf.dialog.ui.CreateFolderDialog
import com.erman.usurf.dialog.ui.FileInformationDialog
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.dialog.ui.SearchDialog
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.MimeUtils
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.FileOpenUtils
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.erman.usurf.utils.logi
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.File

private const val FILE_LIST_GRID_SPAN_COUNT = 1

class DirectoryFragment : Fragment() {
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()
    private val homeViewModel by sharedViewModel<HomeViewModel>()
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var binding: FragmentDirectoryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_directory, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = directoryViewModel
        binding.uiState = directoryViewModel.uiState.value ?: DirectoryUiState()

        directoryViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.uiState = state
            binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
            if (::directoryRecyclerViewAdapter.isInitialized) {
                directoryRecyclerViewAdapter.updateData(state.fileList)
                directoryRecyclerViewAdapter.updateSelection()
            }
        }
        directoryViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is DirectoryUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                    is DirectoryUiEvent.ShowDialog -> handleDialogEvent(event.dialogArgs)
                }
            },
        )
        return binding.root
    }

    private fun handleDialogEvent(args: DialogArgs) {
        when (args) {
            is DialogArgs.RenameDialogArgs -> showRenameDialog(args)
            is DialogArgs.InformationDialogArgs -> showInformationDialog(args)
            is DialogArgs.CreateFolderDialogArgs -> showCreateFolderDialog()
            is DialogArgs.CreateFileDialogArgs -> showCreateFileDialog()
            is DialogArgs.CompressDialogArgs -> showCompressDialog()
            is DialogArgs.OpenFileActivityArgs -> openFileFromArgs(args.path)
            is DialogArgs.ShareActivityArgs -> handleShare(args)
            is DialogArgs.AddFavoriteDialogArgs -> showAddFavoriteDialog(args.path)
            is DialogArgs.FileSearchDialogArgs -> showSearchDialog()
            else -> loge("DirectoryFragment $args")
        }
    }

    private fun showRenameDialog(args: DialogArgs.RenameDialogArgs) {
        val dialog: RenameDialog = RenameDialog.newInstance(args.name)
        dialog.onRenameOkPressedListener = OnRenameOkPressedListener { directoryViewModel.onRenameOkPressed(it) }
        dialogListener.showDialog(dialog)
    }

    private fun showInformationDialog(args: DialogArgs.InformationDialogArgs) {
        dialogListener.showDialog(FileInformationDialog.newInstance(args.file))
    }

    private fun showCreateFolderDialog() {
        val dialog = CreateFolderDialog()
        dialog.onFolderCreateOkPressedListener =
            OnFolderCreateOkPressedListener { directoryViewModel.onFolderCreateOkPressed(it) }
        dialogListener.showDialog(dialog)
    }

    private fun showCreateFileDialog() {
        val dialog = CreateFileDialog()
        dialog.onFileCreateOkPressedListener =
            OnFileCreateOkPressedListener { directoryViewModel.onFileCreateOkPressed(it) }
        dialogListener.showDialog(dialog)
    }

    private fun showCompressDialog() {
        val dialog = CompressDialog()
        dialog.onCompressOkPressedListener =
            OnCompressOkPressedListener { directoryViewModel.onFileCompressOkPressed(it) }
        dialogListener.showDialog(dialog)
    }

    private fun openFileFromArgs(path: String) {
        val isOpened: Boolean =
            FileOpenUtils.openFile(
                context = requireContext(),
                path = path,
                grantWritePermission = true,
                bringToFront = true,
            )
        if (!isOpened) {
            Snackbar.make(binding.root, getString(R.string.unsupported_file), Snackbar.LENGTH_LONG).show()
            loge("Error when opening a file")
        }
    }

    private fun handleShare(args: DialogArgs.ShareActivityArgs) {
        val shareableFiles: List<FileModel> = args.multipleSelectionList.filter { !it.isDirectory }
        val fileUris: ArrayList<Uri> = collectShareableFileUris(shareableFiles)
        showDirectoryShareWarning(args.multipleSelectionList)
        launchShareIntent(fileUris, shareableFiles.map { it.path })
    }

    private fun collectShareableFileUris(files: List<FileModel>): ArrayList<Uri> {
        val uris: ArrayList<Uri> = arrayListOf()
        files.forEach { fileModel ->
            logi("Share: ${fileModel.name}")
            uris.add(
                FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().packageName,
                    File(fileModel.path),
                ),
            )
        }
        return uris
    }

    private fun showDirectoryShareWarning(files: List<FileModel>) {
        val directoryNames = files.filter { it.isDirectory }.map { it.name }
        if (directoryNames.isNotEmpty()) {
            val messages = listOf(getString(R.string.share_directory)) + directoryNames
            Snackbar.make(binding.root, messages.toString(), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun launchShareIntent(
        fileUris: ArrayList<Uri>,
        filePaths: List<String>,
    ) {
        if (fileUris.isEmpty()) return
        val clipData: ClipData =
            ClipData.newRawUri("", fileUris.first()).apply {
                fileUris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
            }
        val shareIntent: Intent =
            if (fileUris.size == 1) {
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUris.first())
                    type = MimeUtils.getMimeTypeForPath(filePaths.first())
                    setClipData(clipData)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    type = MIME_TYPE_ALL
                    setClipData(clipData)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        startActivity(Intent.createChooser(shareIntent, requireContext().getString(R.string.share)))
    }

    private fun showAddFavoriteDialog(path: String) {
        val dialog: AddFavoriteDialog = AddFavoriteDialog.newInstance(path)
        dialog.callbacks =
            object : AddFavoriteDialogCallbacks {
                override fun onDialogShown() {
                    directoryViewModel.turnOffOptionPanel()
                    directoryViewModel.clearMultipleSelection()
                }

                override fun onAddFavorite(
                    path: String,
                    name: String,
                ) {
                    homeViewModel.onFavoriteAdd(path, name)
                }
            }
        dialogListener.showDialog(dialog)
    }

    private fun showSearchDialog() {
        val dialog = SearchDialog()
        dialog.onSearchOkPressedListener = OnSearchOkPressedListener { directoryViewModel.onFileSearchOkPressed(it) }
        dialogListener.showDialog(dialog)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val path: String = DirectoryFragmentArgs.fromBundle(requireArguments()).currentPath ?: ROOT_DIRECTORY
        directoryViewModel.setPath(path)
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater,
                ) {
                    menuInflater.inflate(R.menu.menu_directory, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId == R.id.action_device_wide_search) {
                        directoryViewModel.deviceWideSearch()
                        return true
                    }
                    return false
                }
            },
            viewLifecycleOwner,
        )
        binding.fileListRecyclerView.layoutManager = GridLayoutManager(context, FILE_LIST_GRID_SPAN_COUNT)
        directoryRecyclerViewAdapter =
            DirectoryRecyclerViewAdapter(
                object : FileItemListener {
                    override fun onFileClick(file: FileModel) = directoryViewModel.onFileClick(file)

                    override fun onFileLongClick(file: FileModel): Boolean = directoryViewModel.onFileLongClick(file)
                },
            ).apply {
                showThumbnails = directoryViewModel.shouldShowThumbnails
            }
        binding.fileListRecyclerView.adapter = directoryRecyclerViewAdapter
        binding.fileListRecyclerView.itemAnimator = null
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeRefreshLayout.setDistanceToTriggerSync(resources.getDimensionPixelSize(R.dimen.swipe_refresh_trigger_distance))
        binding.swipeRefreshLayout.setOnRefreshListener { directoryViewModel.onSwipeRefresh() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            dialogListener = context as ShowDialog
        } catch (err: ClassCastException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }

    override fun onResume() {
        super.onResume()
        directoryViewModel.onFragmentResume()
    }
}
