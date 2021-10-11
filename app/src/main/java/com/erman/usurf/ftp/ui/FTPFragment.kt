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
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import org.koin.android.viewmodel.ext.android.viewModel

class FTPFragment : Fragment() {
    private val fTPViewModel by viewModel<FTPViewModel>()
    private lateinit var binding: FragmentFtpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)

        fTPViewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, getString(it), Toast.LENGTH_LONG).show()
        })

        fTPViewModel.storagePaths.observe(viewLifecycleOwner, {
            for (storagePath in it) {
                val radioButton = RadioButton(context)
                radioButton.text = storagePath
                radioButton.id = it.indexOf(storagePath)
                if (storagePath == fTPViewModel.getFtpSelectedPath())
                    radioButton.isChecked = true
                binding.radioButtonGroup.addView(radioButton)
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
            binding.editUserNameTextView.isEnabled = false
            binding.editPasswordTextView.isEnabled = false
            binding.editPortTextView.isEnabled = false
            binding.radioButtonGroup.isGone = true
            binding.connectButton.text = getString(R.string.disconnect)
        }
    }
}