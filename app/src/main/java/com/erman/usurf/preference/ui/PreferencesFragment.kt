package com.erman.usurf.preference.ui

import android.content.Context
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.usurf.R
import com.erman.usurf.activity.model.RefreshNavDrawer
import com.erman.usurf.preference.utils.KEY_PREFERENCE_ROOT_ACCESS
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val KEY_SHOW_HIDDEN_SWITCH: String = "showHiddenFileSwitch"
private const val KEY_SHOW_THUMBNAILS_SWITCH: String = "showThumbnails"
private const val KEY_ASCENDING_ORDER_CHECKBOX: String = "ascendingOrderPreference"
private const val KEY_DESCENDING_ORDER_CHECKBOX: String = "descendingOrderPreference"
private const val KEY_SORT_FILES_LIST_PREFERENCE: String = "sortListPreference"

class PreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var navDrawerRefreshListener: RefreshNavDrawer
    private val preferencesViewModel by viewModel<PreferencesViewModel>()

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        val rootAccessPreference = findPreference<SwitchPreference>(KEY_PREFERENCE_ROOT_ACCESS)
        rootAccessPreference?.setOnPreferenceChangeListener { _, newValue ->
            when (val result = preferencesViewModel.onRootAccessChangeRequested(newValue as Boolean)) {
                is RootAccessChangeResult.Applied -> {
                    navDrawerRefreshListener.refreshStorageButtons()
                    true
                }
                is RootAccessChangeResult.Rejected -> {
                    Snackbar.make(requireView(), getString(result.messageResId), Snackbar.LENGTH_LONG).show()
                    false
                }
            }
        }

        val sortListPreference = findPreference<ListPreference>(KEY_SORT_FILES_LIST_PREFERENCE)
        sortListPreference?.setOnPreferenceChangeListener { _, newValue ->
            sortListPreference.title = newValue.toString()
            preferencesViewModel.onSortPreferenceChanged(newValue.toString())
            true
        }

        findPreference<SwitchPreference>(KEY_SHOW_HIDDEN_SWITCH)?.setOnPreferenceChangeListener { _, newValue ->
            preferencesViewModel.onShowHiddenPreferenceChanged(newValue as Boolean)
            true
        }

        findPreference<SwitchPreference>(KEY_SHOW_THUMBNAILS_SWITCH)?.setOnPreferenceChangeListener { _, newValue ->
            preferencesViewModel.onShowThumbnailsPreferenceChanged(newValue as Boolean)
            true
        }

        val ascendingOrderPreference = findPreference<CheckBoxPreference>(KEY_ASCENDING_ORDER_CHECKBOX)
        val descendingOrderPreference = findPreference<CheckBoxPreference>(KEY_DESCENDING_ORDER_CHECKBOX)

        ascendingOrderPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (descendingOrderPreference!!.isChecked) {
                descendingOrderPreference.isChecked = false
                preferencesViewModel.onAscendingOrderPreferenceChanged(newValue as Boolean)
                true
            } else {
                false
            }
        }

        descendingOrderPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (ascendingOrderPreference!!.isChecked) {
                ascendingOrderPreference.isChecked = false
                preferencesViewModel.onDescendingOrderPreferenceChanged(newValue as Boolean)
                true
            } else {
                false
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            navDrawerRefreshListener = context as RefreshNavDrawer
        } catch (err: ClassCastException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }
}
