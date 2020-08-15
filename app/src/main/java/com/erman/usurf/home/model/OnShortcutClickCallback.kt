package com.erman.usurf.home.model

import android.widget.TextView

interface OnShortcutClickListener {
    fun onClick(shortcut: TextView)
    fun onLongClick(shortcut: TextView)
}