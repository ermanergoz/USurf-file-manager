package com.erman.usurf.home.model

data class StorageItem(
    val path: String,
    val usedPercentage: Int,
    val displayName: String = "",
    val usedSize: String = "",
    val totalSize: String = "",
    val isExternal: Boolean = false,
)
