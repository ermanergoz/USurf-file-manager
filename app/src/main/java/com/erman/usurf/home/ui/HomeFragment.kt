package com.erman.usurf.home.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentHomeBinding
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.utils.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var directoryViewModel: DirectoryViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()
        homeViewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)
        val binding: FragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        directoryViewModel =
            ViewModelProvider(requireActivity(), viewModelFactory).get(DirectoryViewModel::class.java)

        homeViewModel.navigateToDirectory.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {navId ->
                findNavController().navigate(navId)
            }
        })

        homeViewModel.storagePath.observe(viewLifecycleOwner, Observer {
            directoryViewModel.setPath(it)
        })

        homeViewModel.saf.observe(viewLifecycleOwner, Observer {
            //https://developer.android.com/reference/android/support/v4/provider/DocumentFile
            it.getContentIfNotHandled()?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    //If you really do need full access to an entire subtree of documents,
                    this.startActivityForResult(intent, 2)
                }
            }
        })
        binding.lifecycleOwner = this
        binding.viewModel = homeViewModel
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        //To avoid storage buttons from disappearing when resuming the app from background
        homeViewModel.storageButtons.observe(viewLifecycleOwner, Observer {
            val buttonLayoutParams =
                FrameLayout.LayoutParams(520, 200)
            buttonLayoutParams.setMargins(10, 0, 10, 0)
            for (i in it.indices) {
                it[i].lifecycleOwner = this
                it[i].viewModel = homeViewModel
                storageUsageBarLayout.addView(it[i].root, buttonLayoutParams)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        //To avoid java.lang.IllegalStateException: The specified child already has a parent.
        // You must call removeView() on the child's parent first.
        storageUsageBarLayout.removeAllViews()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode === Activity.RESULT_OK) {
            val treeUri = data!!.data

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                requireContext().contentResolver.takePersistableUriPermission(treeUri!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            homeViewModel.saveDocumentTree(treeUri.toString())
        } else {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}