package com.erman.usurf.home.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
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
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.dialog.ui.FavoriteOptionsDialog
import com.erman.usurf.dialog.ui.KitkatRemovableStorageWarningDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.FinishActivity
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.utils.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File

class HomeFragment : Fragment() {
    private val homeViewModel by viewModel<HomeViewModel>()
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var finishActivityListener: FinishActivity
    private lateinit var safListener: StorageAccessFramework
    private lateinit var storageButtonDimensions: HomeStorageButton
    private lateinit var binding: FragmentHomeBinding
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        homeViewModel.navigateToDirectory.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { navId ->
                findNavController().navigate(navId)
            }
        }

        homeViewModel.path.observe(viewLifecycleOwner) {
            directoryViewModel.setPath(it)
        }

        homeViewModel.storageButtons.observe(viewLifecycleOwner) {
            val sideMargin = 8
            val dimensions = storageButtonDimensions.autoSizeButtonDimensions(it.size, sideMargin)
            val buttonLayoutParams = FrameLayout.LayoutParams(dimensions.first, dimensions.second)
            buttonLayoutParams.setMargins(sideMargin, 0, sideMargin, 0)
            it.forEach { button ->
                button.lifecycleOwner = this
                button.viewModel = homeViewModel
                binding.storageUsageBarLayout.addView(button.root, buttonLayoutParams)
            }
        }

        homeViewModel.dialog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { args ->
                when (args) {
                    is DialogArgs.RenameDialogArgs -> dialogListener.showDialog(RenameDialog(args.name))
                    is DialogArgs.OpenFileActivityArgs -> {
                        logd("Open a favorite file")
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = FileProvider.getUriForFile(requireContext(), requireContext().packageName, File(args.path))
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        intent.resolveActivity(requireContext().packageManager)?.let { startActivity(intent) }
                            ?: let { Toast.makeText(context, getString(R.string.unsupported_file), Toast.LENGTH_LONG).show() }
                    }
                    is DialogArgs.FavoriteOptionsDialogArgs -> dialogListener.showDialog(FavoriteOptionsDialog(args.view))
                    is DialogArgs.KitkatRemovableStorageDialogArgs -> dialogListener.showDialog(
                        KitkatRemovableStorageWarningDialog()
                    )
                    is DialogArgs.SAFActivityArgs -> safListener.launchSAF()
                    else -> loge("HomeFragment $args")
                }
            }
        }

        homeViewModel.toastMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { messageId ->
                Toast.makeText(context, getString(messageId), Toast.LENGTH_LONG).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //workaround for displaying home fragment repeatedly until back stack is empty
            finishActivityListener.finishActivity()
        }

        binding.lifecycleOwner = this
        binding.viewModel = homeViewModel
        return binding.root
    }

    private fun runRecyclerViewAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_move_up)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoriteRecyclerViewAdapter = FavoriteRecyclerViewAdapter(homeViewModel)
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.favoriteRecyclerView.adapter = favoriteRecyclerViewAdapter
        binding.favoriteRecyclerView.itemAnimator?.let { it.changeDuration = 0 }//to avoid flickering

        homeViewModel.favorites.observe(viewLifecycleOwner) {
            favoriteRecyclerViewAdapter.updateData(it)
            runRecyclerViewAnimation(binding.favoriteRecyclerView)
        }
    }

    private fun refreshStorageButtons() {
        binding.storageUsageBarLayout.removeAllViews()
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
        binding.storageUsageBarLayout.removeAllViews()
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
