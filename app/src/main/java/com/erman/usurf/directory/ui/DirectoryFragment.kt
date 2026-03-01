package com.erman.usurf.directory.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentDirectoryBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.directory.utils.MIME_TYPE_ALL
import com.erman.usurf.dialog.ui.AddFavoriteDialog
import com.erman.usurf.dialog.ui.CompressDialog
import com.erman.usurf.dialog.ui.CreateFileDialog
import com.erman.usurf.dialog.ui.CreateFolderDialog
import com.erman.usurf.dialog.ui.FileInformationDialog
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.dialog.ui.SearchDialog
import com.erman.usurf.directory.ui.DirectoryUiEvent
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import com.erman.usurf.utils.logi
import org.koin.android.viewmodel.ext.android.sharedViewModel
import java.io.File

class DirectoryFragment : Fragment() {
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()
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
        binding.uiState = directoryViewModel.uiState.value ?: DirectoryUIState.Browsing()

        directoryViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.uiState = state
            if (::directoryRecyclerViewAdapter.isInitialized) {
                directoryRecyclerViewAdapter.updateData(state.fileList)
                directoryRecyclerViewAdapter.updateSelection()
                if (!state.isLoading && state.fileList.isNotEmpty()) {
                    runRecyclerViewAnimation(binding.fileListRecyclerView)
                }
            }
        }

        directoryViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is DirectoryUiEvent.ShowToast ->
                        Toast.makeText(context, getString(event.messageResId), Toast.LENGTH_LONG).show()
                    is DirectoryUiEvent.ShowDialog -> handleDialogEvent(event.dialogArgs)
                }
            },
        )

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!directoryViewModel.onBackPressed()) {
                findNavController().popBackStack()
            }
        }
        return binding.root
    }

    private fun handleDialogEvent(args: DialogArgs) {
        when (args) {
            is DialogArgs.RenameDialogArgs -> dialogListener.showDialog(RenameDialog(args.name))
            is DialogArgs.InformationDialogArgs ->
                dialogListener.showDialog(FileInformationDialog(args.file))
            is DialogArgs.CreateFolderDialogArgs -> dialogListener.showDialog(CreateFolderDialog())
            is DialogArgs.CreateFileDialogArgs -> dialogListener.showDialog(CreateFileDialog())
            is DialogArgs.CompressDialogArgs -> dialogListener.showDialog(CompressDialog())
            is DialogArgs.OpenFileActivityArgs -> {
                logd("Opening a file")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().packageName,
                        File(args.path),
                    )
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                }
                intent.resolveActivity(requireContext().packageManager)?.let { startActivity(intent) }
                    ?: run {
                        Toast.makeText(
                            context,
                            getString(R.string.unsupported_file),
                            Toast.LENGTH_LONG,
                        ).show()
                        loge("Error when opening a file")
                    }
            }
            is DialogArgs.ShareActivityArgs -> {
                val fileUris: ArrayList<Uri> = arrayListOf()
                val messages: MutableList<String> = mutableListOf(getString(R.string.share_directory))
                for (fileModel in args.multipleSelectionList) {
                    if (!fileModel.isDirectory) {
                        logi("Share: ${fileModel.name}")
                        fileUris.add(
                            FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().packageName,
                                File(fileModel.path),
                            ),
                        )
                    } else {
                        messages.add(fileModel.name)
                    }
                }
                if (messages.size > 1) {
                    Toast.makeText(context, messages.toString(), Toast.LENGTH_LONG).show()
                }
                val shareIntent = Intent().apply {
                    logd("Start share activity")
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    type = MIME_TYPE_ALL
                }
                startActivity(
                    Intent.createChooser(shareIntent, requireContext().getString(R.string.share)),
                )
            }
            is DialogArgs.AddFavoriteDialogArgs -> dialogListener.showDialog(AddFavoriteDialog(args.path))
            is DialogArgs.FileSearchDialogArgs -> dialogListener.showDialog(SearchDialog())
            else -> loge("DirectoryFragment $args")
        }
    }

    private fun runRecyclerViewAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.fileListRecyclerView.layoutManager = GridLayoutManager(context, 1)
        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter(directoryViewModel)
        binding.fileListRecyclerView.adapter = directoryRecyclerViewAdapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            dialogListener = context as ShowDialog
        } catch (err: ClassCastException) {
            loge("onAttach $err")
        }
    }

    override fun onResume() {
        super.onResume()
        directoryViewModel.onFragmentResume()
        if (::directoryRecyclerViewAdapter.isInitialized) {
            runRecyclerViewAnimation(binding.fileListRecyclerView)
        }
    }
}
