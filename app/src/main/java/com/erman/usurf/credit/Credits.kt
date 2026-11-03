package com.erman.usurf.credit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.erman.usurf.R

class Credits : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_information, container, false)
        val licenseList: LinearLayout = view.findViewById(R.id.licenseList)
        resources.getStringArray(R.array.license_entries).forEach { entry ->
            val row = inflater.inflate(R.layout.license_row, licenseList, false) as TextView
            row.text = entry
            licenseList.addView(row)
        }
        return view
    }
}
