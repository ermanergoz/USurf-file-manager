package com.erman.usurf.ftp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentFtpBinding
import com.erman.usurf.utils.EventObserver
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val RADIO_BUTTON_PADDING_START: Int = 8
private const val RADIO_BUTTON_ICON_SIZE: Int = 24
private const val RADIO_BUTTON_DRAWABLE_PADDING: Int = 8

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
            state?.storagePaths?.let { items ->
                updateStorageRadioButtons(items)
            }
        }
        ftpViewModel.uiEvents.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is FtpUiEvent.ShowSnackbar ->
                        Snackbar.make(binding.root, getString(event.messageResId), Snackbar.LENGTH_LONG).show()
                    is FtpUiEvent.CopyToClipboard ->
                        copyUrlToClipboard(event.url)
                }
            },
        )
        return binding.root
    }

    private fun updateStorageRadioButtons(items: List<StoragePathItem>) {
        binding.radioButtonGroup.removeAllViews()
        val selectedPath: String? = ftpViewModel.getFtpSelectedPath()
        val density: Float = resources.displayMetrics.density
        items.forEachIndexed { index, item ->
            val radioButton = RadioButton(context)
            radioButton.text = item.displayName
            radioButton.id = index
            radioButton.setPadding(
                (RADIO_BUTTON_PADDING_START * density).toInt(),
                radioButton.paddingTop,
                radioButton.paddingRight,
                radioButton.paddingBottom,
            )
            val iconRes: Int = if (item.isExternal) R.drawable.ic_sd_card else R.drawable.ic_hdd
            val icon = ContextCompat.getDrawable(requireContext(), iconRes)
            val iconSizePx: Int = (RADIO_BUTTON_ICON_SIZE * density).toInt()
            icon?.setBounds(0, 0, iconSizePx, iconSizePx)
            radioButton.setCompoundDrawables(icon, null, null, null)
            radioButton.compoundDrawablePadding = (RADIO_BUTTON_DRAWABLE_PADDING * density).toInt()
            if (item.path == selectedPath) {
                radioButton.isChecked = true
            }
            binding.radioButtonGroup.addView(radioButton)
        }
    }

    private fun copyUrlToClipboard(url: String) {
        val clipboard: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(getString(R.string.url), url)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, getString(R.string.ftp_url_copied), Snackbar.LENGTH_SHORT).show()
    }
}
