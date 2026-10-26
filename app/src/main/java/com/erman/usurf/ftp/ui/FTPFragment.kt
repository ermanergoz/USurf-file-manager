package com.erman.usurf.ftp.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
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
    private var _binding: FragmentFtpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ftp, container, false)
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
                    is FtpUiEvent.CopyToClipboard ->
                        copyUrlToClipboard(event.url)
                    is FtpUiEvent.CopyPassword ->
                        copyPasswordToClipboard(event.password)
                    is FtpUiEvent.ShowStartFailure ->
                        Snackbar.make(binding.root, getString(R.string.ftp_server_start_failed, event.reason), Snackbar.LENGTH_LONG).show()
                    is FtpUiEvent.ShowStopFailure ->
                        Snackbar.make(binding.root, getString(R.string.ftp_server_stop_failed, event.reason), Snackbar.LENGTH_LONG).show()
                }
            }
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
                radioButton.paddingBottom
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

    // Marked sensitive, so Android 13 and later hide it in the clipboard preview.
    private fun copyPasswordToClipboard(password: String) {
        val clipboard: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(getString(R.string.hint_password), password)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, getString(R.string.ftp_password_copied), Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
