package com.erman.usurf.home.ui

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.home.data.Favorite
import com.erman.usurf.R
import com.erman.usurf.databinding.RecyclerFavoriteLayoutBinding
import com.erman.usurf.directory.utils.MARQUEE_REPEAT_LIM
import kotlinx.android.synthetic.main.recycler_favorite_layout.view.*

class FavoriteRecyclerViewAdapter(var viewModel: HomeViewModel) :
    RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteHolder>() {
    var favorites = listOf<Favorite>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteHolder {
        val binding: RecyclerFavoriteLayoutBinding =
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.recycler_favorite_layout, parent, false)

        binding.viewModel = viewModel
        return FavoriteHolder(binding)
    }

    override fun getItemCount(): Int {
        return favorites.count()
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        holder.bindButtons(favorites.elementAt(position))
    }

    inner class FavoriteHolder(var binding: RecyclerFavoriteLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindButtons(favorite: Favorite) {
            itemView.favorite.text = favorite.name
            itemView.favorite.tag = favorite.path
            itemView.favorite.isSingleLine = true
            
            itemView.favorite.ellipsize = TextUtils.TruncateAt.MARQUEE
            //for sliding names if the length is longer than 1 line
            itemView.favorite.isSelected = true
            itemView.favorite.marqueeRepeatLimit = MARQUEE_REPEAT_LIM
            //-1 is for forever
        }
    }

    fun updateData(favorites: List<Favorite>) {
        this.favorites = favorites
        notifyDataSetChanged()
    }
}