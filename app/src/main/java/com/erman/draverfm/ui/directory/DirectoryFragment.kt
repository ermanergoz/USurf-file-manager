package com.erman.draverfm.ui.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.erman.drawerfm.R

class DirectoryFragment : Fragment() {

    private lateinit var directoryViewModel: DirectoryViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        directoryViewModel =
                ViewModelProviders.of(this).get(DirectoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_directory, container, false)
        val textView: TextView = root.findViewById(R.id.text_directory)
        directoryViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}