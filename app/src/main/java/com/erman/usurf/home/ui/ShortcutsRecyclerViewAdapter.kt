package com.erman.usurf.home.ui

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.home.data.Shortcut
import com.erman.usurf.R
import com.erman.usurf.databinding.RecyclerDirectoryLayoutBinding
import com.erman.usurf.databinding.RecyclerShortcutLayoutBinding
import com.erman.usurf.directory.ui.DirectoryRecyclerViewAdapter
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.directory.utils.MARQUEE_CHOICE_KEY
import com.erman.usurf.directory.utils.MARQUEE_REPEAT_LIM
import com.erman.usurf.utils.SHARED_PREF_FILE
import kotlinx.android.synthetic.main.recycler_shortcut_layout.view.*

class ShortcutRecyclerViewAdapter(var viewModel: HomeViewModel) :
    RecyclerView.Adapter<ShortcutRecyclerViewAdapter.ShortcutHolder>() {
    var shortcuts = listOf<Shortcut>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutHolder {
        val binding: RecyclerShortcutLayoutBinding =
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.recycler_shortcut_layout, parent, false)

        binding.viewModel = viewModel
        return ShortcutHolder(binding)
    }

    override fun getItemCount(): Int {
        return shortcuts.count()
    }

    override fun onBindViewHolder(holder: ShortcutHolder, position: Int) {
        holder.bindButtons(shortcuts.elementAt(position))
    }

    inner class ShortcutHolder(var binding: RecyclerShortcutLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindButtons(shortcut: Shortcut) {
            itemView.shortcut.text = shortcut.name
            itemView.shortcut.tag = shortcut.path
            itemView.shortcut.isSingleLine = true
            
            itemView.shortcut.ellipsize = TextUtils.TruncateAt.MARQUEE
            //for sliding names if the length is longer than 1 line
            itemView.shortcut.isSelected = true
            itemView.shortcut.marqueeRepeatLimit = MARQUEE_REPEAT_LIM
            //-1 is for forever
        }
    }

    fun updateData(shortcuts: List<Shortcut>) {
        this.shortcuts = shortcuts
        notifyDataSetChanged()
    }
}