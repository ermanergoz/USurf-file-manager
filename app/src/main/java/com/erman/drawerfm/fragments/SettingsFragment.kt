package com.erman.drawerfm.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat;
import com.erman.drawerfm.R

class SettingsFragment: PreferenceFragmentCompat(){
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

}