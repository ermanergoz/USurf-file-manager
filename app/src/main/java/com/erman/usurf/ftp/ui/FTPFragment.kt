package com.erman.usurf.ftp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import com.erman.usurf.utils.ViewModelFactory

class FTPFragment : Fragment() {
    private lateinit var fTPFragmentViewModel: FTPFragmentViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()
        fTPFragmentViewModel = ViewModelProvider(this, viewModelFactory).get(FTPFragmentViewModel::class.java)
        val binding: FragmentFtpBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)

        val usernameTextView: TextView = binding.root.findViewById(R.id.editUserNameTextView)
        fTPFragmentViewModel.username.observe(viewLifecycleOwner, Observer {
            usernameTextView.text = it
        })

        val passwordTextView: TextView = binding.root.findViewById(R.id.editPasswordTextView)
        fTPFragmentViewModel.password.observe(viewLifecycleOwner, Observer {
            passwordTextView.text = it
        })

        val portTextView: TextView = binding.root.findViewById(R.id.editPortTextView)
        fTPFragmentViewModel.port.observe(viewLifecycleOwner, Observer {
            portTextView.text = it
        })

        fTPFragmentViewModel.openTaskEvent.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        binding.lifecycleOwner = this
        binding.viewModel = fTPFragmentViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //radioButtonGroup.setOnCheckedChangeListener { group, checkedId ->
            //if (checkedId == 0) chosenPath = getStorageDirectories(this)[0]
            //if (checkedId == 1) chosenPath = getStorageDirectories(this)[1]

            //restartService()
        //}
    }
}