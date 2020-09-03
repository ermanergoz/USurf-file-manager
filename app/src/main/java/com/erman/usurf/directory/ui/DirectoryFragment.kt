package com.erman.usurf.directory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentDirectoryBinding
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_directory.*

class DirectoryFragment : Fragment() {
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var directoryRecyclerViewAdapter: DirectoryRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModelFactory = ViewModelFactory()
        directoryViewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        ).get(DirectoryViewModel::class.java)
        val binding: FragmentDirectoryBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_directory, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = directoryViewModel

        directoryViewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!directoryViewModel.onBackPressed()) {
                findNavController().popBackStack()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileListRecyclerView.layoutManager = GridLayoutManager(context, 1)
        directoryRecyclerViewAdapter = DirectoryRecyclerViewAdapter(directoryViewModel)
        fileListRecyclerView.adapter = directoryRecyclerViewAdapter

        directoryViewModel.path.observe(viewLifecycleOwner, Observer {
            directoryRecyclerViewAdapter.updateData(directoryViewModel.getFileList())
        })
    }
}