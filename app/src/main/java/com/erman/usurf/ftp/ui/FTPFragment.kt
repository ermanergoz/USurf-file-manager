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
    private lateinit var fTPViewModel: FTPViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModelFactory = ViewModelFactory()
        fTPViewModel = ViewModelProvider(this, viewModelFactory).get(FTPViewModel::class.java)
        val binding: FragmentFtpBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)

        val usernameTextView: TextView = binding.root.findViewById(R.id.editUserNameTextView)
        fTPViewModel.username.observe(viewLifecycleOwner, Observer {
            usernameTextView.text = it
        })

        val passwordTextView: TextView = binding.root.findViewById(R.id.editPasswordTextView)
        fTPViewModel.password.observe(viewLifecycleOwner, Observer {
            passwordTextView.text = it
        })

        val portTextView: TextView = binding.root.findViewById(R.id.editPortTextView)
        fTPViewModel.port.observe(viewLifecycleOwner, Observer {
            portTextView.text = it
        })

        fTPViewModel.openTaskEvent.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        binding.lifecycleOwner = this
        binding.viewModel = fTPViewModel
        return binding.root
    }
}