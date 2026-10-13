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
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.model.OnRenameOkPressedListener
import com.erman.usurf.dialog.ui.AddFavoriteDialog
import com.erman.usurf.dialog.ui.CompressDialog
import com.erman.usurf.dialog.ui.CreateFileDialog
import com.erman.usurf.dialog.ui.CreateFolderDialog
import com.erman.usurf.dialog.ui.DeleteConfirmationDialog
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
import java.io.File
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

private const val FILE_LIST_GRID_SPAN_COUNT = 1

class DirectoryFragment : Fragment() {
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()
    private val homeViewModel by sharedViewModel<HomeViewModel>()
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private var _binding: FragmentDirectoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_directory, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = directoryViewModel
        binding.uiState = directoryViewModel.uiState.value ?: DirectoryUiState()

        directoryViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.uiState = state
            binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
            if (::directoryRecyclerViewAdapter.isInitialized) {
                directoryRecyclerViewAdapter.updateData(state.fileList)
            }
        }
        directoryViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is DirectoryUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                    is DirectoryUiEvent.ShowDialog -> handleDialogEvent(event.dialogArgs)
                    is DirectoryUiEvent.ConfirmDelete -> dialogListener.showDialog(DeleteConfirmationDialog.newInstance(event.names))
                }
            }
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
        dialogListener.showDialog(CreateFolderDialog())
    }

    private fun showCreateFileDialog() {
        dialogListener.showDialog(CreateFileDialog())
    }

    private fun showCompressDialog() {
        dialogListener.showDialog(CompressDialog())
    }

    private fun openFileFromArgs(path: String) {
        val isOpened: Boolean =
            FileOpenUtils.openFile(
                context = requireContext(),
                path = path,
                grantWritePermission = true,
                bringToFront = true
            )
        if (!isOpened) {
            Snackbar.make(binding.root, getString(R.string.unsupported_file), Snackbar.LENGTH_LONG).show()
            loge("Error when opening a file")
        }
    }

    private fun handleShare(args: DialogArgs.ShareActivityArgs) {
        val fileUris: ArrayList<Uri> = collectShareableFileUris(args.shareablePaths)
        showDirectoryShareWarning(args.directoryNames)
        launchShareIntent(fileUris, args.shareablePaths)
    }

    private fun collectShareableFileUris(paths: List<String>): ArrayList<Uri> {
        val uris: ArrayList<Uri> = arrayListOf()
        paths.forEach { path ->
            logi("Share: ${File(path).name}")
            uris.add(
                FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().packageName,
                    File(path)
                )
            )
        }
        return uris
    }

    private fun showDirectoryShareWarning(directoryNames: List<String>) {
        if (directoryNames.isNotEmpty()) {
            val messages = listOf(getString(R.string.share_directory)) + directoryNames
            Snackbar.make(binding.root, messages.toString(), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun launchShareIntent(fileUris: ArrayList<Uri>, filePaths: List<String>) {
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
        dialogListener.showDialog(AddFavoriteDialog.newInstance(path))
    }

    private fun showSearchDialog() {
        dialogListener.showDialog(SearchDialog())
    }

    private fun registerDialogResultListeners() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.setFragmentResultListener(CreateFolderDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            directoryViewModel.onFolderCreateOkPressed(bundle.getString(CreateFolderDialog.RESULT_NAME).orEmpty())
        }
        fragmentManager.setFragmentResultListener(CreateFileDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            directoryViewModel.onFileCreateOkPressed(bundle.getString(CreateFileDialog.RESULT_NAME).orEmpty())
        }
        fragmentManager.setFragmentResultListener(CompressDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            directoryViewModel.onFileCompressOkPressed(bundle.getString(CompressDialog.RESULT_NAME).orEmpty())
        }
        fragmentManager.setFragmentResultListener(DeleteConfirmationDialog.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            directoryViewModel.confirmDelete()
        }
        fragmentManager.setFragmentResultListener(SearchDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            directoryViewModel.onFileSearchOkPressed(bundle.getString(SearchDialog.RESULT_QUERY).orEmpty())
        }
        fragmentManager.setFragmentResultListener(AddFavoriteDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            val favoritePath = bundle.getString(AddFavoriteDialog.RESULT_PATH).orEmpty()
            val favoriteName = bundle.getString(AddFavoriteDialog.RESULT_NAME).orEmpty()
            val isSuccess = homeViewModel.onFavoriteAdd(favoritePath, favoriteName)
            val messageResId = if (isSuccess) R.string.favorite_created else R.string.unable_to_create_favorite
            Snackbar.make(binding.root, getString(messageResId), Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerDialogResultListeners()
        // Navigate to the argument path on first creation, or when the shared ViewModel has
        // no state yet (e.g. after process death). On a plain recreation the ViewModel already
        // holds the directory the user browsed to, so leave it untouched.
        if (savedInstanceState == null || !directoryViewModel.hasActiveLocation()) {
            val path: String = DirectoryFragmentArgs.fromBundle(requireArguments()).currentPath ?: ROOT_DIRECTORY
            directoryViewModel.setPath(path)
        }
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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
            viewLifecycleOwner
        )
        binding.fileListRecyclerView.layoutManager = GridLayoutManager(context, FILE_LIST_GRID_SPAN_COUNT)
        directoryRecyclerViewAdapter =
            DirectoryRecyclerViewAdapter(
                object : FileItemListener {
                    override fun onFileClick(file: FileModel) = directoryViewModel.onFileClick(file)

                    override fun onFileLongClick(file: FileModel): Boolean = directoryViewModel.onFileLongClick(file)
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
