package com.erman.drawerfm.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.erman.drawerfm.R


class PreferencesFragment : PreferenceFragmentCompat() {

    private lateinit var mPreferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    private var isDarkTheme: Boolean = false


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mPreferences =
            context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference("theme_switch").setOnPreferenceChangeListener { preference, any ->
            this.isDarkTheme = any as Boolean

            true
        }
    }

    override fun onPause() {
        super.onPause()

        var preferencesEditor: SharedPreferences.Editor = mPreferences.edit()

        preferencesEditor.putBoolean("theme switch", isDarkTheme)
        preferencesEditor.apply()
    }
}