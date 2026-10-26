package com.erman.usurf.dialog.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager

/** A pager as tall as its tallest page, so it fits inside a dialog, which gives its content only the height it asks for. */
class WrapContentViewPager(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val tallest =
            (0 until childCount).maxOfOrNull { index ->
                val child = getChildAt(index)
                child.measure(widthMeasureSpec, unspecified)
                child.measuredHeight
            } ?: 0
        val height = View.resolveSize(tallest, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}
