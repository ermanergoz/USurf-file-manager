package com.erman.drawerfm.interfaces

import android.widget.TextView
import java.io.File

interface OnFileClickListener {
    fun onClick(directory: File)
    fun onLongClick(directory: File)
}

interface OnShortcutClickListener {
    fun onClick(shortcut: TextView)
    fun onLongClick(shortcut: TextView)
}