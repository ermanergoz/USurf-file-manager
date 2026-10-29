package com.erman.usurf.directory.ui

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.R
import com.erman.usurf.activity.MainViewModel
import com.erman.usurf.databinding.FragmentDirectoryBinding
import com.erman.usurf.dialog.ui.DeleteConfirmationDialog
import com.erman.usurf.dialog.ui.FileInformationDialog
import com.erman.usurf.dialog.ui.TextInputDialogFragment
import com.erman.usurf.dialog.ui.TextInputRequest
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.directory.utils.FILE_EXTENSION_SEPARATOR
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.directory.utils.MimeUtils
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.FileOpenUtils
import com.erman.usurf.utils.ROOT_DIRECTORY
import com.erman.usurf.utils.ScreenBackCallback
import com.erman.usurf.utils.loge
import com.erman.usurf.utils.logi
import com.erman.usurf.utils.showOnce
import com.google.android.material.snackbar.Snackbar
import java.io.File
import org.koin.androidx.viewmodel.ext.android.activityViewModel

private const val FILE_LIST_GRID_SPAN_COUNT = 1

class DirectoryFragment : Fragment() {
    // The activity's view model, created with the activity's saved state, so a restarted process gives back its folder.
    private val directoryViewModel by activityViewModel<DirectoryViewModel>()
    private val mainViewModel by activityViewModel<MainViewModel>()
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter
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
                directoryRecyclerViewAdapter.updateData(state.files)
            }
        }
        directoryViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is DirectoryUiEvent.ShowSnackbar -> {
                        val message = event.formatArg?.let { getString(event.messageResId, it) } ?: getString(event.messageResId)
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                    is DirectoryUiEvent.ShowRenameDialog -> showRenameDialog(event)
                    is DirectoryUiEvent.ShowInformation -> showInformation(event.files)
                    DirectoryUiEvent.ShowCreateFolderDialog -> showCreateDialog(CreateKind.FOLDER)
                    DirectoryUiEvent.ShowCreateFileDialog -> showCreateDialog(CreateKind.FILE)
                    DirectoryUiEvent.ShowCompressDialog -> showCompressDialog()
                    DirectoryUiEvent.ShowSearchDialog -> showSearchDialog()
                    is DirectoryUiEvent.ShowAddFavoriteDialog -> showAddFavoriteDialog(event.path)
                    is DirectoryUiEvent.OpenFile -> openFile(event.path)
                    is DirectoryUiEvent.Share -> handleShare(event)
                    is DirectoryUiEvent.ConfirmDelete ->
                        show(DeleteConfirmationDialog.newInstance(event.paths), DeleteConfirmationDialog.TAG)
                    DirectoryUiEvent.RequestSafGrant -> mainViewModel.requestSafAccess()
                }
            }
        )
        return binding.root
    }

    private fun showRenameDialog(args: DirectoryUiEvent.ShowRenameDialog) {
        // A file's name is selected without its extension, so typing keeps the extension.
        val nameLength = if (args.isDirectory) null else args.name.lastIndexOf(FILE_EXTENSION_SEPARATOR).takeIf { it > 0 }
        showTextInput(
            TextInputRequest(
                requestKey = RENAME_REQUEST_KEY,
                title = R.string.rename,
                hint = R.string.hint_name,
                defaultText = args.name,
                positiveButton = R.string.rename,
                validateAsName = true,
                payload = bundleOf(PAYLOAD_PATH to args.path),
                selectedLength = nameLength,
                notifiesCancel = true
            )
        )
    }

    // On this screen's own fragment manager, where its result listeners are, so a result reaches this screen.
    private fun show(dialog: DialogFragment, tag: String) {
        showOnce(childFragmentManager, dialog, tag)
    }

    private fun showTextInput(request: TextInputRequest) {
        show(TextInputDialogFragment.newInstance(request), TextInputDialogFragment.TAG)
    }

    private fun showInformation(files: List<FileModel>) {
        show(FileInformationDialog.newInstance(files), FileInformationDialog.TAG)
    }

    private fun showCreateDialog(kind: CreateKind) {
        val request =
            when (kind) {
                CreateKind.FOLDER -> TextInputRequest(
                    requestKey = CREATE_FOLDER_REQUEST_KEY,
                    title = R.string.create_new_folder,
                    hint = R.string.hint_folder_name,
                    defaultText = getString(R.string.new_folder),
                    positiveButton = R.string.create,
                    validateAsName = true
                )
                CreateKind.FILE -> TextInputRequest(
                    requestKey = CREATE_FILE_REQUEST_KEY,
                    title = R.string.create_file,
                    hint = R.string.hint_file_name,
                    defaultText = getString(R.string.new_file),
                    positiveButton = R.string.create,
                    validateAsName = true
                )
            }
        showTextInput(request)
    }

    private fun showCompressDialog() {
        showTextInput(
            TextInputRequest(
                requestKey = COMPRESS_REQUEST_KEY,
                title = R.string.compress,
                hint = R.string.hint_archive_name,
                defaultText = getString(R.string.new_compressed),
                message = R.string.supported_compression_formats,
                positiveButton = R.string.compress,
                validateAsName = true,
                notifiesCancel = true
            )
        )
    }

    private fun openFile(path: String) {
        val isOpened: Boolean =
            FileOpenUtils.openFile(
                context = requireContext(),
                path = path,
                grantWritePermission = true
            )
        if (!isOpened) {
            Snackbar.make(binding.root, getString(R.string.unsupported_file), Snackbar.LENGTH_LONG).show()
            loge("Error when opening a file")
        }
    }

    private fun handleShare(args: DirectoryUiEvent.Share) {
        val fileUris: ArrayList<Uri> = collectShareableFileUris(args.paths)
        showDirectoryShareWarning(args.directoryNames)
        launchShareIntent(fileUris, args.paths)
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
            Snackbar.make(binding.root, resources.folderShareWarning(directoryNames), Snackbar.LENGTH_LONG).show()
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

    // A favourite is named after its folder by default; the top of the file system has no name of its own.
    private fun showAddFavoriteDialog(path: String) {
        showTextInput(
            TextInputRequest(
                requestKey = ADD_FAVORITE_REQUEST_KEY,
                title = R.string.addFavorite,
                hint = R.string.hint_favorite_name,
                defaultText = File(path).name.ifEmpty { getString(R.string.new_favorite) },
                positiveButton = R.string.add,
                validateAsName = true,
                payload = bundleOf(PAYLOAD_PATH to path)
            )
        )
    }

    private fun showSearchDialog() {
        showTextInput(
            TextInputRequest(
                requestKey = SEARCH_REQUEST_KEY,
                title = R.string.device_wide_search,
                hint = R.string.hint_search_query,
                positiveButton = R.string.search,
                validateAsName = false
            )
        )
    }

    private fun registerDialogResultListeners() {
        val fragmentManager = childFragmentManager
        fun onText(requestKey: String, onResult: (Bundle, String) -> Unit) {
            fragmentManager.setFragmentResultListener(requestKey, viewLifecycleOwner) { _, bundle ->
                onResult(bundle, TextInputDialogFragment.textIn(bundle))
            }
        }
        onText(CREATE_FOLDER_REQUEST_KEY) { _, name -> directoryViewModel.onCreateNameEntered(CreateKind.FOLDER, name) }
        onText(CREATE_FILE_REQUEST_KEY) { _, name -> directoryViewModel.onCreateNameEntered(CreateKind.FILE, name) }
        onText(COMPRESS_REQUEST_KEY) { _, name -> directoryViewModel.onFileCompressOkPressed(name) }
        onText(RENAME_REQUEST_KEY) { bundle, name -> directoryViewModel.onRenameOkPressed(bundle.getString(PAYLOAD_PATH).orEmpty(), name) }
        onText(SEARCH_REQUEST_KEY) { _, query -> directoryViewModel.onFileSearchOkPressed(query) }
        onText(ADD_FAVORITE_REQUEST_KEY) { bundle, name ->
            directoryViewModel.onFavoriteAdd(bundle.getString(PAYLOAD_PATH).orEmpty(), name)
        }
        // A cancelled compress or rename tells the view model that the items it was opened for are no longer waiting.
        for (requestKey in listOf(COMPRESS_REQUEST_KEY, RENAME_REQUEST_KEY)) {
            fragmentManager.setFragmentResultListener(TextInputDialogFragment.cancelKeyOf(requestKey), viewLifecycleOwner) { _, _ ->
                directoryViewModel.onSelectionDialogCancelled()
            }
        }
        fragmentManager.setFragmentResultListener(DeleteConfirmationDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            directoryViewModel.confirmDelete(DeleteConfirmationDialog.pathsIn(bundle))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerDialogResultListeners()
        // This screen decides Back first; what it does not use goes on to the activity, which closes the drawer or exits.
        val dispatcher = requireActivity().onBackPressedDispatcher
        dispatcher.addCallback(viewLifecycleOwner, ScreenBackCallback(dispatcher) { directoryViewModel.onBackPressed() })
        // Navigate to the argument path on first creation, or when the view model has no location to show. On a
        // recreation, and after a stopped process, the view model already shows the folder or the search the user left
        // and has listed it, so it is left as it is.
        if (savedInstanceState == null || directoryViewModel.uiState.value?.hasActiveLocation != true) {
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

    override fun onResume() {
        super.onResume()
        directoryViewModel.onScreenResumed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val RENAME_REQUEST_KEY = "rename"
        const val CREATE_FOLDER_REQUEST_KEY = "create_folder"
        const val CREATE_FILE_REQUEST_KEY = "create_file"
        const val COMPRESS_REQUEST_KEY = "compress"
        const val SEARCH_REQUEST_KEY = "search"
        const val ADD_FAVORITE_REQUEST_KEY = "add_favorite"
        const val PAYLOAD_PATH = "path"
    }
}

/** The sentence that names the folders of a shared selection, which are not shared: only files are. */
internal fun Resources.folderShareWarning(folderNames: List<String>): String =
    getQuantityString(R.plurals.share_folders_skipped, folderNames.size, folderNames.joinToString(separator = ", "))
