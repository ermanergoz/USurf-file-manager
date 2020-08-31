package com.erman.usurf.directory.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.erman.usurf.R
import com.erman.usurf.home.utils.KEY_BUNDLE_STORAGE_DIRECTORY

class DirectoryFragment : Fragment() {

    private lateinit var directoryViewModel: DirectoryViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
       // directoryViewModel = ViewModelProviders.of(this).get(DirectoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_directory, container, false)
        //val textView: TextView = root.findViewById(R.id.text_directory)
        //directoryViewModel.text.observe(viewLifecycleOwner, Observer {
        //    textView.text = it
        //})
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e("selected path", requireArguments().getString(KEY_BUNDLE_STORAGE_DIRECTORY)!!)
    }
}