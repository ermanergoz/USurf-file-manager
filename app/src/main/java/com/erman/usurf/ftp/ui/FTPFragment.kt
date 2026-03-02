package com.erman.usurf.ftp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import com.erman.usurf.utils.EventObserver
import com.google.android.material.snackbar.Snackbar
import org.koin.android.viewmodel.ext.android.viewModel

class FTPFragment : Fragment() {
    private val ftpViewModel by viewModel<FTPViewModel>()
    private lateinit var binding: FragmentFtpBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = ftpViewModel
        binding.uiState = ftpViewModel.uiState.value ?: FtpUiState()
        ftpViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.uiState = state
            state?.storagePaths?.let { paths ->
                binding.radioButtonGroup.removeAllViews()
                paths.forEachIndexed { index, path ->
                    val radioButton = RadioButton(context)
                    radioButton.text = path
                    radioButton.id = index
                    if (path == ftpViewModel.getFtpSelectedPath()) {
                        radioButton.isChecked = true
                    }
                    binding.radioButtonGroup.addView(radioButton)
                }
            }
        }
        ftpViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is FtpUiEvent.ShowToast ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                }
            },
        )
        return binding.root
    }
}
