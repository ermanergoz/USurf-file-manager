package com.erman.usurf.directory.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentDirectoryBinding
import com.erman.usurf.dialog.ui.*
import com.erman.usurf.utils.*
import kotlinx.android.synthetic.main.fragment_directory.*
import java.io.File

class DirectoryFragment : Fragment() {
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()
        directoryViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DirectoryViewModel::class.java)
        val binding: FragmentDirectoryBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_directory, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = directoryViewModel

        directoryViewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        directoryViewModel.multipleSelection.observe(viewLifecycleOwner, Observer {
            directoryRecyclerViewAdapter.updateSelection()
        })

        directoryViewModel.openFile.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                logd("Opening a file")
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = FileProvider.getUriForFile(requireContext(), requireContext().packageName, File(args.path))
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.resolveActivity(requireContext().packageManager)?.let { startActivity(intent) }
                    ?: let {
                        Toast.makeText(context, getString(R.string.unsupported_file), Toast.LENGTH_LONG).show()
                        loge("Error when opening a file")
                    }
            }
        })

        directoryViewModel.onCompress.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                dialogListener.showDialog(CompressDialog())
            }
        })

        directoryViewModel.onShare.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                val fileUris: ArrayList<Uri> = arrayListOf()

                for (fileModel in args.multipleSelectionList) {
                    logi("Share: " + fileModel.name)
                    fileUris.add(FileProvider.getUriForFile(requireContext(), requireContext().packageName, //(use your app signature + ".provider" )
                        File(fileModel.path)))  //used this instead of File().toUri to avoid FileUriExposedException
                }
                val shareIntent = Intent().apply {
                    logd("Start share activity")
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    type = "*/*"
                }
                startActivity(Intent.createChooser(shareIntent, requireContext().getString(R.string.share)))
            }
        })

        directoryViewModel.onRename.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(RenameDialog(args.name))
            }
        })

        directoryViewModel.onCreateFolder.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                dialogListener.showDialog(CreateFolderDialog())
            }
        })

        directoryViewModel.onCreateFile.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                dialogListener.showDialog(CreateFileDialog())
            }
        })

        directoryViewModel.onInformation.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(FileInformationDialog(args.file))
            }
        })

        directoryViewModel.onAddShortcut.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(AddShortcutDialog(args.path))
            }
        })

        directoryViewModel.updateDirectoryList.observe(viewLifecycleOwner, Observer {
            directoryRecyclerViewAdapter.updateData(it)
        })

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!directoryViewModel.onBackPressed()) {
                findNavController().popBackStack()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileListRecyclerView.layoutManager = GridLayoutManager(context, 1)
        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter(directoryViewModel)
        fileListRecyclerView.adapter = directoryRecyclerViewAdapter
        fileListRecyclerView.itemAnimator?.let { it.changeDuration = 0 } //to avoid flickering

        directoryViewModel.path.observe(viewLifecycleOwner, Observer {
            directoryRecyclerViewAdapter.updateData(directoryViewModel.getFileList())
        })
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
        directoryRecyclerViewAdapter.updateData(directoryViewModel.getFileList())
    }
}