package com.erman.usurf.directory.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
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

    // Whether any row is selected: while one is, every row says whether it is selected too.
    private var isSelecting: Boolean = false

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
            val row = binding.directoryLayout
            row.isSelected = directory.isSelected
            ViewCompat.setStateDescription(row, row.resources.selectionStateText(directory.isSelected, isSelecting))
        }
    }

    fun updateData(newList: List<FileModel>) {
        val wasSelecting = isSelecting
        isSelecting = newList.any { it.isSelected }
        directoryList =
            updateWithDiff(
                oldList = directoryList,
                newList = newList,
                areItemsTheSame = { oldItem, newItem -> oldItem.path == newItem.path },
                areContentsTheSame = { oldItem, newItem -> oldItem == newItem }
            )
        // A row that did not change says whether it is selected only once the selection starts, and stops when it ends.
        if (wasSelecting != isSelecting) notifyItemRangeChanged(0, directoryList.size)
    }
}
