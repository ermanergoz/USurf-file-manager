package com.erman.usurf.history.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.R
import com.erman.usurf.databinding.FragmentHistoryBinding
import com.erman.usurf.utils.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_history.*

class HistoryFragment : Fragment() {
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelFactory = ViewModelFactory()

        historyViewModel = ViewModelProvider(this, viewModelFactory).get(HistoryViewModel::class.java)
        val binding: FragmentHistoryBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = historyViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView.postDelayed({ scrollView.fullScroll(ScrollView.FOCUS_DOWN) }, 200)
        super.onViewCreated(view, savedInstanceState)
    }
}