package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.drawerfm.R
import com.erman.drawerfm.activities.MainActivity

class MainPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        preferences = context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

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

    override fun onPause() {
        super.onPause()

        val intent: Intent? = context!!.packageManager.getLaunchIntentForPackage(context!!.packageName)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        MainActivity.mainActivity.finish()
        startActivity(intent)
    }
}