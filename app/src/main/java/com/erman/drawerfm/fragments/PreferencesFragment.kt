package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.erman.drawerfm.R
import com.erman.drawerfm.activities.MainActivity

class PreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var mPreferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    private var selectedTheme: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mPreferences =
            context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference("theme_list_preference").setOnPreferenceChangeListener { preference, newValue ->
            this.selectedTheme = newValue.toString()
            true
        }
    }

    override fun onPause() {
        super.onPause()

        var preferencesEditor: SharedPreferences.Editor = mPreferences.edit()

        preferencesEditor.putString("theme choice", selectedTheme)
        preferencesEditor.apply()

        val intent: Intent? = context!!.packageManager
            .getLaunchIntentForPackage(context!!.packageName)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        MainActivity.firsActivity.finish()
        startActivity(intent)
    }
}