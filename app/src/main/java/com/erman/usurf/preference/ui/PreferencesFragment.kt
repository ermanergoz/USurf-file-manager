package com.erman.usurf.preference.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import com.erman.usurf.R
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.preference.utils.*
import com.erman.usurf.activity.model.RefreshNavDrawer
import com.erman.usurf.utils.loge
import org.koin.android.ext.android.inject

class MainPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var navDrawerRefreshListener: RefreshNavDrawer
    private val preferenceProvider: PreferenceProvider by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        val rootAccessPreference = findPreference<SwitchPreference>(KEY_PREFERENCE_ROOT_ACCESS)
        rootAccessPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (RootHandler().isDeviceRooted()) {
                preferenceProvider.editRootAccessPreference(newValue as Boolean)
                navDrawerRefreshListener.refreshStorageButtons()
                true
            } else {
                Toast.makeText(context, getString(R.string.su_not_found), Toast.LENGTH_LONG).show()
                false
            }
        }

        val sortListPreference = findPreference<ListPreference>(KEY_SORT_FILES_LIST_PREFERENCE)

        sortListPreference?.setOnPreferenceChangeListener { _, newValue ->
            sortListPreference.title = newValue.toString()
            preferenceProvider.editFileSortPreference(newValue.toString())
            true
        }

        findPreference<SwitchPreference>(KEY_SHOW_HIDDEN_SWITCH)?.setOnPreferenceChangeListener { _, newValue ->
            preferenceProvider.editShowHiddenPreference(newValue as Boolean)
            true
        }

        findPreference<SwitchPreference>(KEY_SHOW_THUMBNAILS_SWITCH)?.setOnPreferenceChangeListener { _, newValue ->
            preferenceProvider.editShowThumbnailsPreference(newValue as Boolean)
            true
        }

        val ascendingOrderPreference = findPreference<CheckBoxPreference>(KEY_ASCENDING_ORDER_CHECKBOX)
        val descendingOrderPreference = findPreference<CheckBoxPreference>(KEY_DESCENDING_ORDER_CHECKBOX)

        ascendingOrderPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (descendingOrderPreference!!.isChecked) {
                descendingOrderPreference.isChecked = false
                preferenceProvider.editAscendingOrderPreference(newValue as Boolean)
                true
            } else false
        }

        descendingOrderPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (ascendingOrderPreference!!.isChecked) {
                ascendingOrderPreference.isChecked = false
                preferenceProvider.editDescendingOrderPreference(newValue as Boolean)
                true
            } else false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            navDrawerRefreshListener = context as RefreshNavDrawer
        } catch (err: ClassCastException) {
            loge("onAttach $err")
        }
    }
}
