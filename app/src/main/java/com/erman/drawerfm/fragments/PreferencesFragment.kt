package com.erman.drawerfm.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.erman.drawerfm.R
import com.erman.drawerfm.activities.MainActivity

class PreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var mPreferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    private var selectedTheme: String = ""
    lateinit var preferencesEditor: SharedPreferences.Editor
    var isMarqueeEnabled: Boolean = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mPreferences =
            context!!.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        findPreference("theme_list_preference").setOnPreferenceChangeListener { preference, newValue ->
            this.selectedTheme = newValue.toString()

            preferencesEditor = mPreferences.edit()
            preferencesEditor.putString("theme choice", selectedTheme)
            preferencesEditor.apply()

            true
        }

        findPreference("marquee_preference").setOnPreferenceChangeListener { preference, newValue ->
            this.isMarqueeEnabled = newValue as Boolean

            preferencesEditor = mPreferences.edit()
            preferencesEditor.putBoolean("marquee choice", isMarqueeEnabled)
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