package com.erman.drawerfm.adapters

import DirectoryData
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erman.drawerfm.R
import kotlinx.android.synthetic.main.directory_recycler_layout.view.*
import java.text.SimpleDateFormat

class DirectoryRecyclerViewAdapter:
    RecyclerView.Adapter<DirectoryRecyclerViewAdapter.ViewHolder>() {

    var onClickListener: ((DirectoryData) -> Unit)? = null
    var onLongClickListener: ((DirectoryData) -> Unit)? = null
    var directoryList = listOf<DirectoryData>()
    private var dateFormat = SimpleDateFormat("dd MMMM | HH:mm:ss")


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.directory_recycler_layout,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return directoryList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindDirectory(directoryList[position])

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            onClickListener?.invoke(directoryList[adapterPosition])
        }

        override fun onLongClick(p0: View?): Boolean {
            onLongClickListener?.invoke(directoryList[adapterPosition])
            return true
        }

        fun bindDirectory(directoryData: DirectoryData) {
            itemView.nameTextView.text = directoryData.name
            itemView.nameTextView.isSingleLine = true

            if (itemView.context.getSharedPreferences(
                    "com.erman.draverfm",
                    Context.MODE_PRIVATE
                ).getBoolean("marquee choice", true)
            ) {
                itemView.nameTextView.ellipsize =
                    TextUtils.TruncateAt.MARQUEE  //for sliding names if the length is longer than 1 line
                itemView.nameTextView.isSelected = true
                itemView.nameTextView.marqueeRepeatLimit = -1   //-1 is for forever
            }

            if (directoryData.isFolder) {
                itemView.imageView.setImageResource(R.drawable.folder_icon)
                if (directoryData.subFileNum == 0) {
                    itemView.totalSizeTextView.text = "Empty Folder"
                } else {
                    itemView.totalSizeTextView.text = directoryData.subFileNum.toString() + " Files"
                    itemView.lastModifiedTextView.text =
                        dateFormat.format(directoryData.lastModifiedDate)
                }
            } else {
                itemView.imageView.setImageResource(R.drawable.file_icon)
                itemView.totalSizeTextView.visibility = View.VISIBLE
                itemView.totalSizeTextView.text =
                    "${String.format("%.2f", directoryData.sizeInMB)} mb"
                itemView.lastModifiedTextView.text =
                    dateFormat.format(directoryData.lastModifiedDate)
            }
        }

    }

    fun updateData(filesList: List<DirectoryData>) {
        this.directoryList = filesList
        notifyDataSetChanged()
    }
}