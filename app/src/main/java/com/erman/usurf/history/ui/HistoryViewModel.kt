package com.erman.usurf.history.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.erman.usurf.history.model.HistoryModel

class HistoryViewModel(private val historyModel: HistoryModel) : ViewModel() {
    val history = MutableLiveData<String>().apply {
        value = historyModel.readHistoryFile()
    }
}