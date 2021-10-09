package com.erman.usurf.directory.ui

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.directory.utils.MARQUEE_REPEAT_LIM
import androidx.databinding.library.baseAdapters.BR
import com.erman.usurf.databinding.RecyclerDirectoryLayoutBinding
import com.erman.usurf.directory.model.FileModel

class DirectoryRecyclerViewAdapter(var viewModel: DirectoryViewModel) :
    RecyclerView.Adapter<DirectoryRecyclerViewAdapter.ViewHolder>() {
    private var directoryList = listOf<FileModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: RecyclerDirectoryLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.recycler_directory_layout,
            parent, false
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

        fun bindDirectory(directory: FileModel) {
            binding.setVariable(BR.file, directory)
            binding.nameTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
            //for sliding names if the length is longer than 1 line
            binding.nameTextView.isSelected = true
            binding.nameTextView.marqueeRepeatLimit = MARQUEE_REPEAT_LIM   //-1 is for forever
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(filesList: List<FileModel>) {
        this.directoryList = filesList
        notifyDataSetChanged()
    }

    fun updateSelection() {
        for (i in directoryList.indices) {
            notifyItemChanged(i)
        }
    }
}