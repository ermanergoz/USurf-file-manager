package com.erman.usurf.home.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.dialog.ui.ShortcutOptionsDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.FinishActivity
import com.erman.usurf.utils.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.File

class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var shortcutRecyclerViewAdapter: ShortcutRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var finishActivityListener: FinishActivity
    private lateinit var safListener: StorageAccessFramework
    private lateinit var storageButtonDimensions: HomeStorageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()
        homeViewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)
        val binding: FragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        directoryViewModel =
            ViewModelProvider(requireActivity(), viewModelFactory).get(DirectoryViewModel::class.java)

        homeViewModel.navigateToDirectory.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { navId ->
                findNavController().navigate(navId)
            }
        })

        homeViewModel.path.observe(viewLifecycleOwner, Observer {
            directoryViewModel.setPath(it)
        })

        homeViewModel.saf.observe(viewLifecycleOwner, Observer {
            safListener.launchSAF()
        })

        homeViewModel.storageButtons.observe(viewLifecycleOwner, Observer {
            val sideMargin = 8
            val dimensions = storageButtonDimensions.autoSizeButtonDimensions(it.size, sideMargin)
            val buttonLayoutParams = FrameLayout.LayoutParams(dimensions.first, dimensions.second)
            buttonLayoutParams.setMargins(sideMargin, 0, sideMargin, 0)
            it.forEach {button ->
                button.lifecycleOwner = this
                button.viewModel = homeViewModel
                storageUsageBarLayout.addView(button.root, buttonLayoutParams)
            }
        })

        homeViewModel.onShortcutOption.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(ShortcutOptionsDialog(args.view))
            }
        })

        homeViewModel.onRename.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                dialogListener.showDialog(RenameDialog(args.name))
            }
        })

        homeViewModel.openFile.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { args ->
                logd("Open a shortcut file")
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = FileProvider.getUriForFile(requireContext(), requireContext().packageName, File(args.path))
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.resolveActivity(requireContext().packageManager)?.let { startActivity(intent) }
                    ?: let { Toast.makeText(context, getString(R.string.unsupported_file), Toast.LENGTH_LONG).show() }
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //workaround for displaying home fragment repeatedly until back stack is empty
            finishActivityListener.finishActivity()
        }

        binding.lifecycleOwner = this
        binding.viewModel = homeViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shortcutRecyclerViewAdapter = ShortcutRecyclerViewAdapter(homeViewModel)
        shortcutRecyclerView.layoutManager = GridLayoutManager(context, 2)
        shortcutRecyclerView.adapter = shortcutRecyclerViewAdapter
        shortcutRecyclerView.itemAnimator?.let { it.changeDuration = 0 }//to avoid flickering

        homeViewModel.shortcuts.observe(viewLifecycleOwner, Observer {
            shortcutRecyclerViewAdapter.updateData(it)
        })
    }

    private fun refreshStorageButtons() {
        storageUsageBarLayout.removeAllViews()
        homeViewModel.createStorageButtons()
    }

    override fun onResume() {
        super.onResume()
        //To avoid storage buttons from disappearing when resuming the app from background
        //And also, to refresh it after preference change
        refreshStorageButtons()
    }

    override fun onPause() {
        super.onPause()
        //To avoid java.lang.IllegalStateException: The specified child already has a parent.
        // You must call removeView() on the child's parent first.
        storageUsageBarLayout.removeAllViews()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            dialogListener = context as ShowDialog
            finishActivityListener = context as FinishActivity
            safListener = context as StorageAccessFramework
            storageButtonDimensions = context as HomeStorageButton
        } catch (err: ClassCastException) {
            err.printStackTrace()
        }
    }
}