package com.erman.usurf.home.ui

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.directory.utils.MARQUEE_REPEAT_LIM
import com.erman.usurf.databinding.RecyclerFavoriteLayoutBinding
import com.erman.usurf.home.model.FavoriteItem
import com.erman.usurf.utils.updateWithDiff

class FavoriteRecyclerViewAdapter(private val listener: FavoriteItemListener) :
    RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteHolder>() {
    private var favorites = listOf<FavoriteItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): FavoriteHolder {
        val itemBinding =
            DataBindingUtil.inflate<RecyclerFavoriteLayoutBinding>(
                LayoutInflater.from(parent.context),
                R.layout.recycler_favorite_layout,
                parent,
                false,
            )
        itemBinding.listener = listener
        return FavoriteHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return favorites.count()
    }

    override fun onBindViewHolder(
        holder: FavoriteHolder,
        position: Int,
    ) {
        holder.bind(favorites.elementAt(position))
    }

    inner class FavoriteHolder(private val binding: RecyclerFavoriteLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(favorite: FavoriteItem) {
            binding.favoriteItem = favorite
            binding.executePendingBindings()
            binding.favorite.isSingleLine = true
            binding.favorite.ellipsize = TextUtils.TruncateAt.MARQUEE
            binding.favorite.isSelected = true
            binding.favorite.marqueeRepeatLimit = MARQUEE_REPEAT_LIM
        }
    }

    fun updateData(newFavorites: List<FavoriteItem>) {
        favorites =
            updateWithDiff(
                oldList = favorites,
                newList = newFavorites,
                areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id },
                areContentsTheSame = { oldItem, newItem ->
                    oldItem.name == newItem.name && oldItem.path == newItem.path
                },
            )
    }
}
