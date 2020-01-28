package com.erman.drawerfm.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.erman.drawerfm.R

class FileListPreferencesFragment: PreferenceFragmentCompat()
{
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_file_list, rootKey)
    }
}