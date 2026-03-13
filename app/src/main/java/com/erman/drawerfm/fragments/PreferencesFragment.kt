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

class PreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var mPreferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    private var selectedTheme: String = ""
    lateinit var preferencesEditor: SharedPreferences.Editor
    var isMarqueeEnabled: Boolean = true
    var isRootAccessEnabled: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mPreferences =
            context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference<ListPreference>("theme_list_preference")?.setOnPreferenceChangeListener { preference, newValue ->
            this.selectedTheme = newValue.toString()

            preferencesEditor = mPreferences.edit()
            preferencesEditor.putString("theme choice", selectedTheme)
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("marquee_preference")?.setOnPreferenceChangeListener { preference, newValue ->
            this.isMarqueeEnabled = newValue as Boolean

            preferencesEditor = mPreferences.edit()
            preferencesEditor.putBoolean("marquee choice", isMarqueeEnabled)
            preferencesEditor.apply()

            true
        }

        findPreference<SwitchPreference>("root_access")?.setOnPreferenceChangeListener { preference, newValue ->
            this.isRootAccessEnabled = newValue as Boolean

            preferencesEditor = mPreferences.edit()
            preferencesEditor.putBoolean("root access", isRootAccessEnabled)
            preferencesEditor.apply()

            true
        }
    }

    override fun onPause() {
        super.onPause()

        val intent: Intent? = context!!.packageManager
            .getLaunchIntentForPackage(context!!.packageName)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        MainActivity.mainActivity.finish()
        startActivity(intent)
    }
}