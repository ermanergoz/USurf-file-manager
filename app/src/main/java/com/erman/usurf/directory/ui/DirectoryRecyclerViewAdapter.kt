package com.erman.usurf.directory.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.databinding.RecyclerDirectoryLayoutBinding
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.updateWithDiff

class DirectoryRecyclerViewAdapter(private val listener: FileItemListener) :
    RecyclerView.Adapter<DirectoryRecyclerViewAdapter.ViewHolder>() {
    private var directoryList = listOf<FileModel>()
    var showThumbnails: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: RecyclerDirectoryLayoutBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.recycler_directory_layout,
                parent,
                false
            )
        binding.listener = listener
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = directoryList.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindDirectory(directoryList[position])
    }

    inner class ViewHolder(var binding: RecyclerDirectoryLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindDirectory(directory: FileModel) {
            binding.setVariable(BR.file, directory)
            binding.showThumbnails = showThumbnails
            binding.executePendingBindings()
        }
    }

    fun updateData(newList: List<FileModel>) {
        directoryList =
            updateWithDiff(
                oldList = directoryList,
                newList = newList,
                areItemsTheSame = { oldItem, newItem -> oldItem.path == newItem.path },
                areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
            )
    }
}
