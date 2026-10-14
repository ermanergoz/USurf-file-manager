package com.erman.usurf.preference.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.erman.usurf.R
import com.erman.usurf.activity.MainViewModel
import com.erman.usurf.activity.model.RefreshNavDrawer
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.preference.utils.KEY_PREFERENCE_ROOT_ACCESS
import com.erman.usurf.preference.utils.KEY_PREFERENCE_SD_CARD_ACCESS
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val KEY_SHOW_HIDDEN_SWITCH: String = "showHiddenFileSwitch"
private const val KEY_SHOW_THUMBNAILS_SWITCH: String = "showThumbnails"
private const val KEY_ASCENDING_ORDER_CHECKBOX: String = "ascendingOrderPreference"
private const val KEY_DESCENDING_ORDER_CHECKBOX: String = "descendingOrderPreference"
private const val KEY_SORT_FILES_LIST_PREFERENCE: String = "sortListPreference"

class PreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var navDrawerRefreshListener: RefreshNavDrawer
    private val preferencesViewModel by viewModel<PreferencesViewModel>()
    private val mainViewModel by activityViewModel<MainViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        findPreference<SwitchPreference>(KEY_PREFERENCE_ROOT_ACCESS)?.setOnPreferenceChangeListener { _, newValue ->
            val isEnableRequest = newValue as Boolean
            preferencesViewModel.onRootAccessChangeRequested(isEnableRequest)
            // Turning root on waits for the su answer off the main thread; its result sets the switch.
            !isEnableRequest
        }

        findPreference<Preference>(KEY_PREFERENCE_SD_CARD_ACCESS)?.apply {
            isVisible = mainViewModel.canOfferSafGrant()
            setOnPreferenceClickListener {
                (activity as? StorageAccessFramework)?.launchSAF()
                true
            }
        }

        tintPreferenceIcons()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferencesViewModel.isRootAccessCheckPending.observe(viewLifecycleOwner) { isPending ->
            findPreference<SwitchPreference>(KEY_PREFERENCE_ROOT_ACCESS)?.isEnabled = !isPending
        }
        preferencesViewModel.rootAccessChangeResult.observe(viewLifecycleOwner, EventObserver(::showRootAccessChangeResult))
    }

    private fun showRootAccessChangeResult(result: RootAccessChangeResult) {
        when (result) {
            is RootAccessChangeResult.Applied -> {
                findPreference<SwitchPreference>(KEY_PREFERENCE_ROOT_ACCESS)?.isChecked = result.isRootAccessEnabled
                navDrawerRefreshListener.refreshStorageButtons()
            }
            is RootAccessChangeResult.Rejected -> {
                Snackbar.make(requireView(), getString(result.messageResId), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun tintPreferenceIcons() {
        val color: Int = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val tintList: ColorStateList = ColorStateList.valueOf(color)
        val screen: PreferenceGroup = preferenceScreen
        for (i in 0 until screen.preferenceCount) {
            val preference = screen.getPreference(i)
            if (preference is PreferenceGroup) {
                for (j in 0 until preference.preferenceCount) {
                    preference.getPreference(j).icon?.setTintList(tintList)
                }
            } else {
                preference.icon?.setTintList(tintList)
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
