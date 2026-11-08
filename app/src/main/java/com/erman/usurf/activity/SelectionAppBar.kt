package com.erman.usurf.activity

import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.erman.usurf.R
import com.erman.usurf.directory.ui.selectionTitle

/**
 * The app bar while items are selected on the folder screen: it says how many, and its drawer button becomes Close,
 * which runs [onClose]. Once none is selected the bar is the drawer's again.
 */
class SelectionAppBar(
    private val activity: AppCompatActivity,
    private val toolbar: Toolbar,
    private val drawerToggle: ActionBarDrawerToggle,
    onClose: () -> Unit
) {
    // Whether the bar shows a selection now, instead of the drawer button and the screen's title.
    private var isShown: Boolean = false

    init {
        // Shown in place of the drawer button whenever its indicator is off, and run when it is tapped then.
        drawerToggle.setHomeAsUpIndicator(R.drawable.ic_close)
        drawerToggle.toolbarNavigationClickListener = View.OnClickListener { onClose() }
    }

    /**
     * Shows [count] selected items; at none, gives the bar back, titled [screenTitle] when the screen on now is one whose
     * title the bar has to set again, and left as navigation titled it when [screenTitle] is null.
     */
    fun show(count: Int, screenTitle: CharSequence?) {
        if (count > 0) {
            drawerToggle.isDrawerIndicatorEnabled = false
            toolbar.navigationContentDescription = activity.getString(R.string.close_selection)
            activity.supportActionBar?.title = activity.resources.selectionTitle(count)
            isShown = true
        } else if (isShown) {
            drawerToggle.isDrawerIndicatorEnabled = true
            screenTitle?.let { activity.supportActionBar?.title = it }
            isShown = false
        }
    }
}
