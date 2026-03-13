package com.erman.usurf.home.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.model.DialogArgs
import com.erman.usurf.dialog.model.FavoriteOptionsDialogListener
import com.erman.usurf.dialog.model.OnRenameOkPressedListener
import com.erman.usurf.dialog.ui.FavoriteOptionsDialog
import com.erman.usurf.dialog.ui.RenameDialog
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.FileOpenUtils
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val STORAGE_BUTTON_HORIZONTAL_MARGIN: Int = 4
private const val STORAGE_BUTTON_VERTICAL_MARGIN: Int = 4
private const val FAVORITE_GRID_SPAN_COUNT: Int = 2

class HomeFragment : Fragment() {
    private val homeViewModel by viewModel<HomeViewModel>()
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var safListener: StorageAccessFramework
    private lateinit var storageButtonDimensions: HomeStorageButton
    private lateinit var binding: FragmentHomeBinding

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
        var lastRenderedStorageItems: List<StorageItem>? = null
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.storageItems !== lastRenderedStorageItems) {
                lastRenderedStorageItems = state.storageItems
                binding.storageUsageBarLayout.removeAllViews()
                if (state.storageItems.isNotEmpty()) {
                    createStorageButtons(state.storageItems)
                }
            }
        }
    }

    private fun setupUiEventsObserver() {
        homeViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is HomeUiEvent.NavigateToDirectory ->
                        findNavController().navigate(MobileNavigationDirections.globalActionNavDirectory(event.path))
                    is HomeUiEvent.ShowDialog -> handleDialog(event.dialogArgs)
                    is HomeUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                }
            },
        )
    }

    private fun createStorageButtons(items: List<StorageItem>) {
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
        val isOpened: Boolean =
            FileOpenUtils.openFile(
                context = requireContext(),
                path = path,
                grantWritePermission = true,
            )
        if (!isOpened) {
            Snackbar.make(binding.root, getString(R.string.unsupported_file), Snackbar.LENGTH_LONG).show()
        }
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
        binding.favoriteRecyclerView.layoutManager = GridLayoutManager(context, FAVORITE_GRID_SPAN_COUNT)
        binding.favoriteRecyclerView.adapter = favoriteRecyclerViewAdapter
        binding.favoriteRecyclerView.itemAnimator = null

        homeViewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            favoriteRecyclerViewAdapter.updateData(favorites)
            updateEmptyState(favorites.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyFavoritesContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.favoriteRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }
}
