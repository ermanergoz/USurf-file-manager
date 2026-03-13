package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.drawerfm.common.*
import com.erman.drawerfm.R
import com.erman.drawerfm.activities.FragmentActivity

class FileListPreferencesFragment(var currentPath: String) : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_file_list, rootKey)
        preferences = context!!.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)

        val sortListPreference = findPreference<ListPreference>(KEY_SORT_FILES_LIST_PREFERENCE)

        sortListPreference?.setOnPreferenceChangeListener { preference, newValue ->

            sortListPreference.title = newValue.toString()

            preferencesEditor = preferences.edit()
            preferencesEditor.putString(PREFERENCE_FILE_SORT, newValue.toString())
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>(KEY_SHOW_HIDDEN_SWITCH)?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean(PREFERENCE_SHOW_HIDDEN, newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        val showFilesOnlySwitch = findPreference<SwitchPreference>(KEY_SHOW_FILES_ONLY_SWITCH)
        val showFoldersOnlySwitch = findPreference<SwitchPreference>(KEY_SHOW_FOLDERS_ONLY_SWITCH)

        showFilesOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if (showFoldersOnlySwitch!!.isChecked) {
                showFoldersOnlySwitch.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_SHOW_FOLDERS_ONLY, false)
            }
            preferencesEditor.putBoolean(PREFERENCE_SHOW_FILES_ONLY, newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        showFoldersOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if (showFilesOnlySwitch!!.isChecked) {
                showFilesOnlySwitch.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_SHOW_FILES_ONLY, false)
            }
            preferencesEditor.putBoolean(PREFERENCE_SHOW_FOLDERS_ONLY, newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        val ascendingOrderPreference = findPreference<CheckBoxPreference>(KEY_ASCENDING_ORDER_CHECKBOX)
        val descendingOrderPreference = findPreference<CheckBoxPreference>(KEY_DESCENDING_ORDER_CHECKBOX)

        ascendingOrderPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (descendingOrderPreference!!.isChecked) {
                descendingOrderPreference.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_DESCENDING_ORDER, false)
                preferencesEditor.putBoolean(PREFERENCE_ASCENDING_ORDER, newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        descendingOrderPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (ascendingOrderPreference!!.isChecked) {
                ascendingOrderPreference.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_ASCENDING_ORDER, false)
                preferencesEditor.putBoolean(PREFERENCE_DESCENDING_ORDER, newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        val showFilesOnTop = findPreference<CheckBoxPreference>(KEY_FILES_ON_TOP_CHECKBOX)
        val showFoldersOnTop = findPreference<CheckBoxPreference>(KEY_FOLDERS_ON_TOP_CHECKBOX)

        showFilesOnTop?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (showFoldersOnTop!!.isChecked) {
                showFoldersOnTop.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_FOLDERS_ON_TOP, false)
                preferencesEditor.putBoolean(PREFERENCE_FILES_ON_TOP, newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        showFoldersOnTop?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (showFilesOnTop!!.isChecked) {
                showFilesOnTop.isChecked = false
                preferencesEditor.putBoolean(PREFERENCE_FILES_ON_TOP, false)
                preferencesEditor.putBoolean(PREFERENCE_FOLDERS_ON_TOP, newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }
    }

    override fun onPause() {
        super.onPause()

        var intent = Intent(context, FragmentActivity::class.java)
        intent.putExtra(KEY_INTENT_PATH, currentPath)
        startActivity(intent)
    }
}