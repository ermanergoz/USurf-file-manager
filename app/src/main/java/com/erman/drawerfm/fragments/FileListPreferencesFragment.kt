package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.drawerfm.R
import com.erman.drawerfm.activities.FragmentActivity

class FileListPreferencesFragment(var currentPath: String) : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_file_list, rootKey)
        preferences = context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        val sortListPreference = findPreference<ListPreference>("sortListPreference")

        sortListPreference?.setOnPreferenceChangeListener { preference, newValue ->

            sortListPreference.title = newValue.toString()

            preferencesEditor = preferences.edit()
            preferencesEditor.putString("sortFileMode", newValue.toString())
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("showHiddenFileSwitch")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean("showHidden", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        val showFilesOnlySwitch = findPreference<SwitchPreference>("showFilesOnlySwitch")
        val showFoldersOnlySwitch = findPreference<SwitchPreference>("showFoldersOnlySwitch")

        showFilesOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if (showFoldersOnlySwitch!!.isChecked) {
                showFoldersOnlySwitch.isChecked = false
                preferencesEditor.putBoolean("showFoldersOnly", false)
            }
            preferencesEditor.putBoolean("showFilesOnly", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        showFoldersOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if (showFilesOnlySwitch!!.isChecked) {
                showFilesOnlySwitch.isChecked = false
                preferencesEditor.putBoolean("showFilesOnly", false)
            }
            preferencesEditor.putBoolean("showFoldersOnly", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        val ascendingOrderPreference = findPreference<CheckBoxPreference>("ascendingOrderPreference")
        val descendingOrderPreference = findPreference<CheckBoxPreference>("descendingOrderPreference")

        ascendingOrderPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (descendingOrderPreference!!.isChecked) {
                descendingOrderPreference.isChecked = false
                preferencesEditor.putBoolean("descendingOrder", false)
                preferencesEditor.putBoolean("ascendingOrder", newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        descendingOrderPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (ascendingOrderPreference!!.isChecked) {
                ascendingOrderPreference.isChecked = false
                preferencesEditor.putBoolean("ascendingOrder", false)
                preferencesEditor.putBoolean("descendingOrder", newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        val showFilesOnTop = findPreference<CheckBoxPreference>("showFilesOnTopPreference")
        val showFoldersOnTop = findPreference<CheckBoxPreference>("showFoldersOnTopPreference")

        showFilesOnTop?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (showFoldersOnTop!!.isChecked) {
                showFoldersOnTop.isChecked = false
                preferencesEditor.putBoolean("showFoldersOnTop", false)
                preferencesEditor.putBoolean("showFilesOnTop", newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }

        showFoldersOnTop?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()

            if (showFilesOnTop!!.isChecked) {
                showFilesOnTop.isChecked = false
                preferencesEditor.putBoolean("showFilesOnTop", false)
                preferencesEditor.putBoolean("showFoldersOnTop", newValue as Boolean)
                preferencesEditor.apply()

                true
            } else false
        }
    }

    override fun onPause() {
        super.onPause()

        var intent = Intent(context, FragmentActivity::class.java)
        intent.putExtra("path", currentPath)
        startActivity(intent)
    }
}