package com.erman.usurf.directory.ui

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.erman.usurf.R
import com.erman.usurf.databinding.RecyclerDirectoryLayoutBinding
import com.erman.usurf.directory.model.FileModel
import com.erman.usurf.utils.MARQUEE_REPEAT_LIM
import com.erman.usurf.utils.updateWithDiff

class DirectoryRecyclerViewAdapter(private val listener: FileItemListener) :
    RecyclerView.Adapter<DirectoryRecyclerViewAdapter.ViewHolder>() {
    private var directoryList = listOf<FileModel>()
    var showThumbnails: Boolean = true

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding: RecyclerDirectoryLayoutBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.recycler_directory_layout,
                parent,
                false,
            )
        binding.listener = listener
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return directoryList.count()
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bindDirectory(directoryList[position])
    }

    inner class ViewHolder(var binding: RecyclerDirectoryLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindDirectory(directory: FileModel) {
            binding.setVariable(BR.file, directory)
            binding.showThumbnails = showThumbnails
            binding.nameTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
            // for sliding names if the length is longer than 1 line
            binding.nameTextView.isSelected = true
            binding.nameTextView.marqueeRepeatLimit = MARQUEE_REPEAT_LIM // -1 is for forever
        }
    }

    fun updateData(newList: List<FileModel>) {
        directoryList =
            updateWithDiff(
                oldList = directoryList,
                newList = newList,
                areItemsTheSame = { oldItem, newItem -> oldItem.path == newItem.path },
                areContentsTheSame = { oldItem, newItem -> oldItem == newItem },
            )
    }

    fun updateSelection() {
        for (i in directoryList.indices) {
            notifyItemChanged(i)
        }
    }
}
