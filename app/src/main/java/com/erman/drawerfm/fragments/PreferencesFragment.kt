package com.erman.drawerfm.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat;
import com.erman.drawerfm.R

class PreferencesFragment: PreferenceFragmentCompat(){
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}