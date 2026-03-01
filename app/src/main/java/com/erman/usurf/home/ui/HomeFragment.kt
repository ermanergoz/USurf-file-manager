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
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.ui.FavoriteOptionsDialog
import com.erman.usurf.dialog.ui.KitkatRemovableStorageWarningDialog
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.home.utils.STORAGE_BUTTON_HORIZONTAL_MARGIN
import com.erman.usurf.home.utils.STORAGE_BUTTON_VERTICAL_MARGIN
import com.erman.usurf.utils.loge
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File

class HomeFragment : Fragment() {
    private val homeViewModel by viewModel<HomeViewModel>()
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var safListener: StorageAccessFramework
    private lateinit var storageButtonDimensions: HomeStorageButton
    private lateinit var binding: FragmentHomeBinding
    private val directoryViewModel by sharedViewModel<DirectoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = homeViewModel

        setupUiEventsObserver()
        setupStorageButtonsObserver()

        return binding.root
    }

    private fun setupStorageButtonsObserver() {
        homeViewModel.storageButtons.observe(viewLifecycleOwner) { buttons ->
            if (buttons != null) {
                binding.storageUsageBarLayout.removeAllViews()
                createStorageButtons(buttons)
            }
        }
    }

    private fun setupUiEventsObserver() {
        homeViewModel.uiEvents.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { e ->
                when (e) {
                    is HomeUiEvent.NavigateToDirectory -> {
                        directoryViewModel.setPath(e.path)
                        findNavController().navigate(e.actionId)
                    }
                    is HomeUiEvent.ShowDialog -> handleDialog(e.dialogArgs)
                    is HomeUiEvent.ShowToast ->
                        Toast.makeText(context, getString(e.messageResId), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createStorageButtons(buttons: List<StorageButtonBinding>) {
        val dimensions =
            storageButtonDimensions.autoSizeButtonDimensions(
                buttons.size,
                STORAGE_BUTTON_HORIZONTAL_MARGIN,
            )
        val buttonLayoutParams = FrameLayout.LayoutParams(dimensions.first, dimensions.second)
        buttonLayoutParams.setMargins(
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN,
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN,
        )
        buttons.forEach { button ->
            button.lifecycleOwner = this
            button.viewModel = homeViewModel
            binding.storageUsageBarLayout.addView(button.root, buttonLayoutParams)
        }
    }

    private fun handleDialog(args: DialogArgs) {
        when (args) {
            is DialogArgs.RenameDialogArgs -> dialogListener.showDialog(RenameDialog(args.name))
            is DialogArgs.OpenFileActivityArgs -> openFile(args.path)
            is DialogArgs.FavoriteOptionsDialogArgs -> dialogListener.showDialog(FavoriteOptionsDialog(args.view))
            is DialogArgs.KitkatRemovableStorageDialogArgs -> dialogListener.showDialog(KitkatRemovableStorageWarningDialog())
            is DialogArgs.SAFActivityArgs -> safListener.launchSAF()
            else -> loge("HomeFragment $args")
        }
    }

    private fun openFile(path: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName,
                File(path),
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        intent.resolveActivity(requireContext().packageManager)
            ?.let { startActivity(intent) }
            ?: Toast.makeText(
                context,
                getString(R.string.unsupported_file),
                Toast.LENGTH_LONG,
            ).show()
    }

    private fun runRecyclerViewAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_move_up)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        favoriteRecyclerViewAdapter = FavoriteRecyclerViewAdapter(homeViewModel)
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.favoriteRecyclerView.adapter = favoriteRecyclerViewAdapter
        binding.favoriteRecyclerView.itemAnimator?.changeDuration = 0

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
        refreshStorageButtons()
    }

    override fun onPause() {
        super.onPause()
        binding.storageUsageBarLayout.removeAllViews()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            dialogListener = context as ShowDialog
            safListener = context as StorageAccessFramework
            storageButtonDimensions = context as HomeStorageButton
        } catch (err: ClassCastException) {
            err.printStackTrace()
        }
    }
}
