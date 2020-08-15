package com.erman.usurf.ftp.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import com.erman.usurf.ftp.model.FtpCallback
import com.erman.usurf.ftp.model.ConnectionLiveData
import kotlinx.android.synthetic.main.fragment_ftp.*

class FTPFragment : Fragment() {

    private lateinit var fTPFragmentViewModel: FTPFragmentViewModel
    private lateinit var connectionLiveData: ConnectionLiveData
    private lateinit var ftpCallback: FtpCallback

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        fTPFragmentViewModel = ViewModelProviders.of(this).get(FTPFragmentViewModel::class.java)
        val binding: FragmentFtpBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)

        connectionLiveData = ConnectionLiveData(requireContext())
        connectionLiveData.observe(viewLifecycleOwner, Observer {
            updateConnectionStatus(it)
            updateURLText(it)
        })

        //binding.lifecycleOwner = this
        //binding.viewModel = fTPFragmentViewModel


        return binding.root
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            statusTextView.text = getString(R.string.connected)
            statusTextView.setTextColor(Color.GREEN)
        } else {
            statusTextView.text = getString(R.string.no_connection)
            statusTextView.setTextColor(Color.RED)
        }
    }

    private fun updateURLText(isConnected: Boolean) {
        fTPFragmentViewModel.ipAddress.observe(viewLifecycleOwner, Observer {
            if (isConnected)
                urlTextView.text = it
            else
                urlTextView.text = ""
        })
    }

    private fun setOnLongClickListeners() {
        userNameTextView.setOnLongClickListener {
            //val newFragment = EditDialog(getString(R.string.edit_username))
            //newFragment.show(fragmentManager, "")
            true
        }

        passwordTextView.setOnLongClickListener {
            //val newFragment = EditPasswordDialog(getString(R.string.edit_password))
            //newFragment.show(fragmentManager, "")
            true
        }

        portTextView.setOnLongClickListener {
            //val newFragment = EditPortDialog(getString(R.string.edit_port))
            //newFragment.show(fragmentManager, "")
            true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            ftpCallback = context as FtpCallback
        } catch (err: ClassCastException) {
            err.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectButton.setOnClickListener {
            ftpCallback.ftpListener()
        }

        radioButtonGroup.setOnCheckedChangeListener { group, checkedId ->
            //if (checkedId == 0) chosenPath = getStorageDirectories(this)[0]
            //if (checkedId == 1) chosenPath = getStorageDirectories(this)[1]

            //restartService()
        }
    }
}