package com.erman.usurf.activity

/** What the activity is asked to do once, for whichever screen asked. */
sealed class MainUiEvent {
    /** Open the system picker for the SD card's document tree; the grant it returns reaches the folder screen's retry. */
    data object LaunchSafPicker : MainUiEvent()
}
