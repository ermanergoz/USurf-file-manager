package com.erman.usurf.directory.model

import android.widget.TextView
import java.io.File

interface OnFileClickListener {
    fun onClick(directory: File)
    fun onLongClick(directory: File)
}
