package com.erman.usurf.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.activity.MainViewModel
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.databinding.StorageButtonBinding
import com.erman.usurf.dialog.ui.TextInputDialogFragment
import com.erman.usurf.dialog.ui.TextInputRequest
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.home.model.StorageItem
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.FileOpenUtils
import com.erman.usurf.utils.showOnce
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val FAVORITE_GRID_SPAN_COUNT: Int = 2

class HomeFragment : Fragment() {
    private val homeViewModel by viewModel<HomeViewModel>()
    private val mainViewModel by activityViewModel<MainViewModel>()
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

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

                    HomeUiEvent.RequestSafGrant -> mainViewModel.requestSafAccess()

                    is HomeUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                }
            }
        )
    }

    // Each card an equal share of the row's width; the row is as tall as the tallest card's text needs.
    private fun createStorageButtons(items: List<StorageItem>) {
        val margin: Int = resources.getDimensionPixelSize(R.dimen.layout_margin_4)
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
            val cardParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(margin, margin, margin, margin)
                }
            binding.storageUsageBarLayout.addView(buttonBinding.root, cardParams)
        }
    }

    private fun showSafGrantMenu(card: View) {
        PopupMenu(requireContext(), card).apply {
            menu.add(R.string.grant_sd_card_access)
            setOnMenuItemClickListener {
                mainViewModel.requestSafAccess()
                true
            }
        }.show()
    }

    private fun showFavoriteOptions(favoritePath: String, favoriteName: String) {
        showOnce(childFragmentManager, FavoriteOptionsSheet.newInstance(favoritePath, favoriteName), FavoriteOptionsSheet.TAG)
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
        showOnce(childFragmentManager, TextInputDialogFragment.newInstance(request), TextInputDialogFragment.TAG)
    }

    private fun registerDialogResultListeners() {
        // This screen's own fragment manager, the one its dialogs are shown on.
        val fragmentManager = childFragmentManager
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val RENAME_FAVORITE_REQUEST_KEY = "rename_favorite"
        const val PAYLOAD_PATH = "path"
    }
}
