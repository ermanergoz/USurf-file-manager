package com.erman.usurf.home.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.model.FavoriteOptionsDialogListener
import com.erman.usurf.dialog.model.OnRenameOkPressedListener
import com.erman.usurf.dialog.ui.FavoriteOptionsDialog
import com.erman.usurf.dialog.ui.KitkatRemovableStorageWarningDialog
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.home.utils.STORAGE_BUTTON_HORIZONTAL_MARGIN
import com.erman.usurf.home.utils.STORAGE_BUTTON_VERTICAL_MARGIN
import com.erman.usurf.utils.loge
import com.google.android.material.snackbar.Snackbar
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
        homeViewModel.storageItems.observe(viewLifecycleOwner) { items ->
            if (items != null) {
                binding.storageUsageBarLayout.removeAllViews()
                createStorageButtons(items)
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
                        Snackbar.make(binding.root, getString(e.messageResId), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createStorageButtons(items: List<com.erman.usurf.home.model.StorageItem>) {
        val dimensions =
            storageButtonDimensions.autoSizeButtonDimensions(
                items.size,
                STORAGE_BUTTON_HORIZONTAL_MARGIN,
            )
        val buttonLayoutParams = FrameLayout.LayoutParams(dimensions.first, dimensions.second)
        buttonLayoutParams.setMargins(
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN,
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN,
        )
        items.forEach { item ->
            val buttonBinding: StorageButtonBinding =
                DataBindingUtil.inflate(
                    layoutInflater,
                    R.layout.storage_button,
                    binding.storageUsageBarLayout,
                    false,
                )
            buttonBinding.lifecycleOwner = viewLifecycleOwner
            buttonBinding.viewModel = homeViewModel
            buttonBinding.storageItem = item
            binding.storageUsageBarLayout.addView(buttonBinding.root, buttonLayoutParams)
        }
    }

    private fun handleDialog(args: DialogArgs) {
        when (args) {
            is DialogArgs.OpenFileActivityArgs -> openFile(args.path)
            is DialogArgs.FavoriteOptionsDialogArgs -> showFavoriteOptionsDialog(args)
            is DialogArgs.KitkatRemovableStorageDialogArgs ->
                dialogListener.showDialog(KitkatRemovableStorageWarningDialog())
            is DialogArgs.SAFActivityArgs -> safListener.launchSAF()
            else -> loge("HomeFragment $args")
        }
    }

    private fun showFavoriteOptionsDialog(args: DialogArgs.FavoriteOptionsDialogArgs) {
        val dialog: FavoriteOptionsDialog = FavoriteOptionsDialog.newInstance(args.favoritePath, args.favoriteName)
        dialog.listener =
            object : FavoriteOptionsDialogListener {
                override fun onRenameButtonClick(
                    path: String,
                    currentName: String,
                ) {
                    homeViewModel.setRenameMode()
                    val renameDialog: RenameDialog = RenameDialog.newInstance(currentName)
                    renameDialog.onRenameOkPressedListener =
                        OnRenameOkPressedListener { newName ->
                            homeViewModel.onRenameFavoriteOkPressed(path, newName)
                        }
                    dialogListener.showDialog(renameDialog)
                }

                override fun onRename(
                    path: String,
                    newName: String,
                ) {
                    homeViewModel.onRenameFavoriteOkPressed(path, newName)
                }

                override fun onDelete(path: String) {
                    homeViewModel.deleteFavorite(path)
                }

                override fun onDismiss() {
                    homeViewModel.turnOffRenameMode()
                }

                override fun getUiState() = homeViewModel.uiState
            }
        dialogListener.showDialog(dialog)
    }

    private fun openFile(path: String) {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data =
                    FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().packageName,
                        File(path),
                    )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
        intent.resolveActivity(requireContext().packageManager)
            ?.let { startActivity(intent) }
            ?: Snackbar.make(binding.root, getString(R.string.unsupported_file), Snackbar.LENGTH_LONG).show()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        favoriteRecyclerViewAdapter =
            FavoriteRecyclerViewAdapter(
                object : FavoriteItemListener {
                    override fun onFavoriteClick(path: String) = homeViewModel.onFavoriteClick(path)

                    override fun onFavoriteLongClick(
                        path: String,
                        name: String,
                    ): Boolean = homeViewModel.onFavoriteLongClick(path, name)
                },
            )
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.favoriteRecyclerView.adapter = favoriteRecyclerViewAdapter
        binding.favoriteRecyclerView.itemAnimator = null

        homeViewModel.favorites.observe(viewLifecycleOwner) {
            favoriteRecyclerViewAdapter.updateData(it)
        }
    }

    private fun refreshStorageButtons() {
        binding.storageUsageBarLayout.removeAllViews()
        homeViewModel.refreshStorageItems()
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
            loge("onAttach $err")
        }
    }
}
