package com.erman.usurf.home.ui

import androidx.annotation.StringRes
import com.erman.usurf.home.model.StorageItem

/** What the home screen shows besides the favourites: the storage volumes. */
data class HomeUiState(val storageItems: List<StorageItem> = emptyList())

/**
 * What the home screen is asked to do once. A dialog or navigation event is emitted only synchronously, from the user
 * intent that asks for it; an operation that completes later emits [ShowSnackbar] or a new state, never a dialog, so
 * no dialog opens on a screen the user has left or over one the user opened since. The one exception is
 * [RequestSafGrant], the permission request: a write the SD card refuses asks for the grant it needs and is retried
 * once after it, so the request continues the user's own intent.
 */
sealed class HomeUiEvent {
    data class NavigateToDirectory(val path: String) : HomeUiEvent()

    /** Offer what can be done with the favourite at [path], named [name]. */
    data class ShowFavoriteOptions(val path: String, val name: String) : HomeUiEvent()

    data class OpenFile(val path: String) : HomeUiEvent()

    /** Ask the user to grant the SD card's document tree before its files are changed. */
    data object RequestSafGrant : HomeUiEvent()

    data class ShowSnackbar(@StringRes val messageResId: Int) : HomeUiEvent()
}
