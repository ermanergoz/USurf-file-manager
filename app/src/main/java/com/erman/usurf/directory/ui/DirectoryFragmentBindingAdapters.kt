package com.erman.usurf.directory.ui

import android.view.View
import androidx.databinding.BindingAdapter
import com.erman.usurf.R

private const val CREATE_MENU_ANIM_DURATION_MS = 200L

/** Fades the view in when it becomes [visible] and out when it stops being so; its first state is set at once. */
@BindingAdapter("fadeVisible")
fun setFadeVisible(view: View, visible: Boolean) {
    val wasVisible = view.getTag(R.id.tag_prev_create_fab_visible) as? Boolean
    view.setTag(R.id.tag_prev_create_fab_visible, visible)
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

/** Grows the view in when it becomes [visible] and shrinks it out when it stops being so; its first state is set at once. */
@BindingAdapter("popVisible")
fun setPopVisible(view: View, visible: Boolean) {
    val wasVisible = view.getTag(R.id.tag_prev_menu_expanded_fab_visible) as? Boolean
    view.setTag(R.id.tag_prev_menu_expanded_fab_visible, visible)
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
