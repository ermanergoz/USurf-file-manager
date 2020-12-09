package com.erman.usurf.ftp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.isGone
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import com.erman.usurf.utils.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_ftp.*

class FTPFragment : Fragment() {
    private lateinit var fTPViewModel: FTPViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()

        fTPViewModel = ViewModelProvider(this, viewModelFactory).get(FTPViewModel::class.java)
        val binding: FragmentFtpBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)

        fTPViewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        fTPViewModel.storagePaths.observe(viewLifecycleOwner, Observer {
            for (storagePath in it) {
                val radioButton = RadioButton(context)
                radioButton.text = storagePath
                radioButton.id = it.indexOf(storagePath)
                if (storagePath == fTPViewModel.getFtpSelectedPath())
                    radioButton.isChecked = true
                radioButtonGroup.addView(radioButton)
            }
        })

        binding.lifecycleOwner = this
        binding.viewModel = fTPViewModel
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        //a workaround to fix the problem of button text not updating on resume with data binding
        if (fTPViewModel.getServerStatus()) {
            editUserNameTextView.isEnabled = false
            editPasswordTextView.isEnabled = false
            editPortTextView.isEnabled = false
            radioButtonGroup.isGone = true
            connectButton.text = getString(R.string.disconnect)
        }
    }
}