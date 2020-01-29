package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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

        findPreference<ListPreference>("sort_list_preference")?.setOnPreferenceChangeListener { preference, newValue ->
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

        var showFilesOnlySwitch= findPreference<SwitchPreference>("showFilesOnlySwitch")
        var showFoldersOnlySwitch = findPreference<SwitchPreference>("showFoldersOnlySwitch")

        showFilesOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if(showFoldersOnlySwitch!!.isChecked)
            {
                showFoldersOnlySwitch.isChecked = false
                preferencesEditor.putBoolean("showFoldersOnly", false)
            }

            preferencesEditor.putBoolean("showFilesOnly", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        showFoldersOnlySwitch?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            if(showFilesOnlySwitch!!.isChecked) {
                showFilesOnlySwitch.isChecked = false
                preferencesEditor.putBoolean("showFilesOnly", false)
            }

            preferencesEditor.putBoolean("showFoldersOnly", newValue as Boolean)
            preferencesEditor.apply()

            true
        }
    }

    override fun onPause() {
        super.onPause()

        var intent = Intent(context, FragmentActivity::class.java)
        intent.putExtra("path", currentPath)
        startActivity(intent)
    }
}