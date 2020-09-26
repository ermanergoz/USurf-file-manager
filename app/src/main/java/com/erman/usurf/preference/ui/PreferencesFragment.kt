package com.erman.usurf.preference.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.erman.usurf.MainApplication
import com.erman.usurf.R
import java.io.File


class MainPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var preferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.usurf"
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        preferences = requireContext().getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference<SwitchPreference>("root_access")?.setOnPreferenceChangeListener { preference, newValue ->
            preferencesEditor = preferences.edit()
            preferencesEditor.putBoolean("root access", newValue as Boolean)
            preferencesEditor.apply()

            true
        }

        findPreference<Preference>("clear_logs")?.setOnPreferenceClickListener {
            if (File(MainApplication.appContext.getExternalFilesDir(null)?.absolutePath + File.separator + "logs").deleteRecursively())
                Toast.makeText(context, getString(R.string.cleared), Toast.LENGTH_LONG).show()
            true
        }
    }
}