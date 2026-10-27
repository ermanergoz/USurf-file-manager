package com.erman.usurf.home.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.ui.TextInputDialogFragment
import com.erman.usurf.dialog.ui.TextInputRequest
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.FileOpenUtils
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

private const val STORAGE_BUTTON_HORIZONTAL_MARGIN: Int = 4
private const val STORAGE_BUTTON_VERTICAL_MARGIN: Int = 4
private const val FAVORITE_GRID_SPAN_COUNT: Int = 2

class HomeFragment : Fragment() {
    private val homeViewModel by sharedViewModel<HomeViewModel>()
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private lateinit var dialogListener: ShowDialog
    private lateinit var safListener: StorageAccessFramework
    private lateinit var storageButtonDimensions: HomeStorageButton
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = homeViewModel

        setupUiEventsObserver()
        setupStorageButtonsObserver()

        return binding.root
    }

    private fun setupStorageButtonsObserver() {
        var lastRenderedStorageItems: List<StorageItem>? = null
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.storageItems != lastRenderedStorageItems) {
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
                    is HomeUiEvent.ShowFavoriteOptions -> showFavoriteOptions(event.path, event.name)
                    is HomeUiEvent.OpenFile -> openFile(event.path)
                    HomeUiEvent.RequestSafGrant -> safListener.launchSAF()
                    is HomeUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun createStorageButtons(items: List<StorageItem>) {
        val containerHorizontalPadding: Int = binding.root.paddingStart + binding.root.paddingEnd
        val dimensions =
            storageButtonDimensions.autoSizeButtonDimensions(
                items.size,
                STORAGE_BUTTON_HORIZONTAL_MARGIN,
                containerHorizontalPadding
            )
        val buttonLayoutParams = FrameLayout.LayoutParams(dimensions.first, dimensions.second)
        buttonLayoutParams.setMargins(
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN,
            STORAGE_BUTTON_HORIZONTAL_MARGIN,
            STORAGE_BUTTON_VERTICAL_MARGIN
        )
        items.forEach { item ->
            val buttonBinding: StorageButtonBinding =
                DataBindingUtil.inflate(
                    layoutInflater,
                    R.layout.storage_button,
                    binding.storageUsageBarLayout,
                    false
                )
            buttonBinding.lifecycleOwner = viewLifecycleOwner
            buttonBinding.viewModel = homeViewModel
            buttonBinding.storageItem = item
            buttonBinding.storageButton.setOnLongClickListener { card ->
                homeViewModel.canOfferSafGrant(item.path).also { canGrant -> if (canGrant) showSafGrantMenu(card) }
            }
            binding.storageUsageBarLayout.addView(buttonBinding.root, buttonLayoutParams)
        }
    }

    private fun showSafGrantMenu(card: View) {
        PopupMenu(requireContext(), card).apply {
            menu.add(R.string.grant_sd_card_access)
            setOnMenuItemClickListener {
                safListener.launchSAF()
                true
            }
        }.show()
    }

    private fun showFavoriteOptions(favoritePath: String, favoriteName: String) {
        dialogListener.showDialog(FavoriteOptionsSheet.newInstance(favoritePath, favoriteName))
    }

    private fun showRenameFavoriteDialog(path: String, currentName: String) {
        val request =
            TextInputRequest(
                requestKey = RENAME_FAVORITE_REQUEST_KEY,
                title = R.string.rename,
                hint = R.string.hint_favorite_name,
                defaultText = currentName,
                positiveButton = R.string.rename,
                validateAsName = true,
                payload = bundleOf(PAYLOAD_PATH to path)
            )
        dialogListener.showDialog(TextInputDialogFragment.newInstance(request))
    }

    private fun registerDialogResultListeners() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.setFragmentResultListener(FavoriteOptionsSheet.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            val path = FavoriteOptionsSheet.pathIn(bundle)
            when (FavoriteOptionsSheet.optionIn(bundle)) {
                FavoriteOption.OPEN -> homeViewModel.onFavoriteOpenChosen(path)
                // Renaming is a second step, the name dialog, so the options never stay in a rename mode of their own.
                FavoriteOption.RENAME -> showRenameFavoriteDialog(path, FavoriteOptionsSheet.nameIn(bundle))
                FavoriteOption.REMOVE -> homeViewModel.deleteFavorite(path)
                null -> Unit
            }
        }
        fragmentManager.setFragmentResultListener(RENAME_FAVORITE_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            homeViewModel.onRenameFavoriteOkPressed(bundle.getString(PAYLOAD_PATH).orEmpty(), TextInputDialogFragment.textIn(bundle))
        }
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
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerDialogResultListeners()

        favoriteRecyclerViewAdapter =
            FavoriteRecyclerViewAdapter(
                object : FavoriteItemListener {
                    override fun onFavoriteClick(item: FavoriteItem) = homeViewModel.onFavoriteClick(item)

                    override fun onFavoriteLongClick(path: String, name: String): Boolean = homeViewModel.onFavoriteLongClick(path, name)
                }
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

    // The cards show the space used now; the state changes, and the cards are built again, only when it differs.
    override fun onResume() {
        super.onResume()
        homeViewModel.refreshStorageItems()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val RENAME_FAVORITE_REQUEST_KEY = "rename_favorite"
        const val PAYLOAD_PATH = "path"
    }
}
