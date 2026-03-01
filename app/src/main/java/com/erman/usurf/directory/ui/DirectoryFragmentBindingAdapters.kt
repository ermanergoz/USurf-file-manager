package com.erman.usurf.directory.ui

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("loadingVisibility")
fun setLoadingVisibility(view: View, isLoading: Boolean) {
    view.visibility = if (isLoading) View.VISIBLE else View.GONE
}

@BindingAdapter("contentVisibility")
fun setContentVisibility(view: View, isLoading: Boolean) {
    view.visibility = if (isLoading) View.GONE else View.VISIBLE
}

@BindingAdapter("confirmationBarVisibility")
fun setConfirmationBarVisibility(view: View, state: DirectoryUIState?) {
    view.visibility = if (state != null && state.isInCopyOrMoveMode) View.VISIBLE else View.GONE
}

@BindingAdapter("optionBarVisibility")
fun setOptionBarVisibility(view: View, state: DirectoryUIState?) {
    val visible = state != null &&
        state.common.isOptionsPanelVisible &&
        !state.isInCopyOrMoveMode
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("moreMenuVisibility")
fun setMoreMenuVisibility(view: View, state: DirectoryUIState?) {
    view.visibility = if (state != null && state.common.isMoreMenuVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("pathVisibility")
fun setPathVisibility(view: View, state: DirectoryUIState?) {
    view.visibility = if (state != null && state.isSearchMode) View.GONE else View.VISIBLE
}

@BindingAdapter("createFabVisibility")
fun setCreateFabVisibility(view: View, state: DirectoryUIState?) {
    val visible = state != null &&
        !state.common.isOptionsPanelVisible &&
        !state.common.isLoading
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("menuExpandedFabVisibility")
fun setMenuExpandedFabVisibility(view: View, state: DirectoryUIState?) {
    val visible = state != null &&
        state.common.isCreateMenuExpanded &&
        !state.common.isOptionsPanelVisible &&
        !state.common.isLoading
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("moreButtonVisibility")
fun setMoreButtonVisibility(view: View, state: DirectoryUIState?) {
    view.visibility = if (state != null && state.common.isInRoot) View.GONE else View.VISIBLE
}
