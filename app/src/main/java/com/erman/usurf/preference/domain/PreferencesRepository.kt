package com.erman.usurf.preference.domain

interface PreferencesRepository {
    fun getRootAccessPreference(): Boolean

    fun setRootAccessPreference(value: Boolean)

    fun getFileSortPreference(): String?

    fun setFileSortPreference(value: String)

    fun getShowHiddenPreference(): Boolean

    fun setShowHiddenPreference(value: Boolean)

    fun getShowThumbnailsPreference(): Boolean

    fun setShowThumbnailsPreference(value: Boolean)

    /** Stores the sort order; ascending is the order that is not descending. */
    fun setDescendingOrderPreference(value: Boolean)

    fun getDescendingOrderPreference(): Boolean
}
