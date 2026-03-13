package com.erman.usurf.utils

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

fun <T> RecyclerView.Adapter<*>.updateWithDiff(
    oldList: List<T>,
    newList: List<T>,
    areItemsTheSame: (T, T) -> Boolean,
    areContentsTheSame: (T, T) -> Boolean,
): List<T> {
    val diffResult =
        DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size

                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(
                    oldPosition: Int,
                    newPosition: Int,
                ): Boolean = areItemsTheSame(oldList[oldPosition], newList[newPosition])

                override fun areContentsTheSame(
                    oldPosition: Int,
                    newPosition: Int,
                ): Boolean = areContentsTheSame(oldList[oldPosition], newList[newPosition])
            },
        )
    diffResult.dispatchUpdatesTo(this)
    return newList
}
