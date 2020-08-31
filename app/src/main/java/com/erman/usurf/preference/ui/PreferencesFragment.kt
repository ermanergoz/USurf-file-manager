package com.erman.usurf.preference.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.usurf.R

class MainPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.usurf"
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        preferences = requireContext().getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference<ListPreference>("theme_list_preference")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putString("theme choice", newValue.toString())
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("marquee_preference")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean("marquee choice", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("root_access")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean("root access", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("grid_view")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean("grid view", newValue as Boolean)
            preferencesEditor.apply()

            true
        }
    }
}