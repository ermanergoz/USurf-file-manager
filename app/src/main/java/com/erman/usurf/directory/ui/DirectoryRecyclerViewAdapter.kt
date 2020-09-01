package com.erman.usurf.directory.ui

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.directory.utils.MARQUEE_CHOICE_KEY
import com.erman.usurf.directory.utils.MARQUEE_REPEAT_LIM
import com.erman.usurf.utils.SHARED_PREF_FILE
import kotlinx.android.synthetic.main.recycler_directory_layout.view.*
import java.io.File
import androidx.databinding.library.baseAdapters.BR
import com.erman.usurf.databinding.RecyclerDirectoryLayoutBinding

class DirectoryRecyclerViewAdapter(var viewModel: DirectoryViewModel) :
    RecyclerView.Adapter<DirectoryRecyclerViewAdapter.ViewHolder>() {
    var directoryList = listOf<File>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: RecyclerDirectoryLayoutBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.recycler_directory_layout,
                parent,
                false
            )
        binding.viewModel = viewModel
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return directoryList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindDirectory(directoryList[position])
    }

    inner class ViewHolder(var binding: RecyclerDirectoryLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindDirectory(directory: File) {
            binding.setVariable(BR.file, directory)
            if (itemView.context.getSharedPreferences(
                    SHARED_PREF_FILE,
                    Context.MODE_PRIVATE
                ).getBoolean(MARQUEE_CHOICE_KEY, true)
            ) {
                itemView.nameTextView.ellipsize =
                    TextUtils.TruncateAt.MARQUEE  //for sliding names if the length is longer than 1 line
                itemView.nameTextView.isSelected = true
                itemView.nameTextView.marqueeRepeatLimit = MARQUEE_REPEAT_LIM   //-1 is for forever
            }
        }
    }

    fun updateData(filesList: List<File>) {
        this.directoryList = filesList
        notifyDataSetChanged()
    }
}