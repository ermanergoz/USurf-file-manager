package com.erman.drawerfm.interfaces

import java.io.File

interface OnItemClickListener {
    fun onClick(directory: File)
    fun onLongClick(directory: File)
}