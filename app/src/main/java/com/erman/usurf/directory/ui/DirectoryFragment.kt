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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
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

        directoryViewModel.onFileSearch.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(SearchDialog())
            }
        })

        directoryViewModel.updateDirectoryList.observe(viewLifecycleOwner, Observer {
            directoryRecyclerViewAdapter.updateData(it)
            runRecyclerViewAnimation(fileListRecyclerView)
        })

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!directoryViewModel.onBackPressed()) {
                //goes to home fragment because it is annoying to navigate to the
                //last opened fragment after directory fragment
                findNavController().navigate(R.id.global_action_nav_home)
            }
        }
        return binding.root
    }

    private fun runRecyclerViewAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileListRecyclerView.layoutManager = GridLayoutManager(context, 1)
        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter(directoryViewModel)
        fileListRecyclerView.adapter = directoryRecyclerViewAdapter

        directoryViewModel.path.observe(viewLifecycleOwner, Observer {
            directoryViewModel.getFileList()
            runRecyclerViewAnimation(fileListRecyclerView)
        })

        directoryViewModel.fileSearchQuery.observe(viewLifecycleOwner, Observer {
            directoryViewModel.getSearchedFiles()
            runRecyclerViewAnimation(fileListRecyclerView)
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
        //this has to be done in fragment since we need to do this in lifecycle function
        //this is to prevent option panel from closing when moving/copying files
        //when we are not copying/moving, it is annoying to keep it open
        var isCopyMode = false
        var isMoveMode = false

        directoryViewModel.copyMode.value?.let {
            isCopyMode = it
        }
        directoryViewModel.moveMode.value?.let {
            isMoveMode = it
        }
        if (!isCopyMode && !isMoveMode)
            directoryViewModel.turnOffOptionPanel()

        directoryViewModel.getFileList()
        runRecyclerViewAnimation(fileListRecyclerView)
    }
}