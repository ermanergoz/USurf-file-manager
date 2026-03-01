package com.erman.usurf.directory.ui

import android.view.View
import androidx.databinding.BindingAdapter

private const val CREATE_MENU_ANIM_DURATION_MS = 200L

private val createFabVisibleState = mutableMapOf<View, Boolean>()
private val menuExpandedFabVisibleState = mutableMapOf<View, Boolean>()

@BindingAdapter("loadingVisibility")
fun setLoadingVisibility(
    view: View,
    isLoading: Boolean,
) {
    view.visibility = if (isLoading) View.VISIBLE else View.GONE
}

@BindingAdapter("contentVisibility")
fun setContentVisibility(
    view: View,
    isLoading: Boolean,
) {
    view.visibility = if (isLoading) View.GONE else View.VISIBLE
}

@BindingAdapter("confirmationBarVisibility")
fun setConfirmationBarVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    view.visibility = if (state != null && state.isInCopyOrMoveMode) View.VISIBLE else View.GONE
}

@BindingAdapter("optionBarVisibility")
fun setOptionBarVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    val visible =
        state != null &&
            state.common.isOptionsPanelVisible &&
            !state.isInCopyOrMoveMode
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("moreMenuVisibility")
fun setMoreMenuVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    view.visibility = if (state != null && state.common.isMoreMenuVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("pathVisibility")
fun setPathVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    view.visibility = if (state != null && state.isSearchMode) View.GONE else View.VISIBLE
}

@BindingAdapter("createFabVisibility")
fun setCreateFabVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    val visible =
        state != null &&
            !state.common.isOptionsPanelVisible &&
            !state.common.isLoading
    val wasVisible = createFabVisibleState[view]
    createFabVisibleState[view] = visible
    if (wasVisible == visible) return
    view.animate().cancel()
    if (visible) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(CREATE_MENU_ANIM_DURATION_MS)
            .start()
    } else {
        if (wasVisible == null) {
            view.visibility = View.GONE
            return
        }
        view.animate()
            .alpha(0f)
            .setDuration(CREATE_MENU_ANIM_DURATION_MS)
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
            }
            .start()
    }
}

@BindingAdapter("menuExpandedFabVisibility")
fun setMenuExpandedFabVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    val visible =
        state != null &&
            state.common.isCreateMenuExpanded &&
            !state.common.isOptionsPanelVisible &&
            !state.common.isLoading
    val wasVisible = menuExpandedFabVisibleState[view]
    menuExpandedFabVisibleState[view] = visible
    if (wasVisible == visible) return
    view.animate().cancel()
    if (visible) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0f
        view.scaleY = 0f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(CREATE_MENU_ANIM_DURATION_MS)
            .start()
    } else {
        if (wasVisible == null) {
            view.visibility = View.GONE
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            return
        }
        view.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(CREATE_MENU_ANIM_DURATION_MS)
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            .start()
    }
}

@BindingAdapter("moreButtonVisibility")
fun setMoreButtonVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    view.visibility = if (state != null && state.common.isInRoot) View.GONE else View.VISIBLE
}

@BindingAdapter("emptyFolderVisibility")
fun setEmptyFolderVisibility(
    view: View,
    state: DirectoryUIState?,
) {
    val visible = state != null && !state.isLoading && state.fileList.isEmpty()
    view.visibility = if (visible) View.VISIBLE else View.GONE
}
