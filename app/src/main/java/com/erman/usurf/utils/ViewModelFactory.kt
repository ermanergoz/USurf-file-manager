package com.erman.usurf.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.ui.FTPViewModel
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.ui.HomeViewModel

//This is to create models with arguments
class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T /*T is the view model class*/ =
        with(modelClass) {
            when {
                isAssignableFrom(FTPViewModel::class.java) ->
                    FTPViewModel(FtpModel())
                isAssignableFrom(HomeViewModel::class.java) ->
                    HomeViewModel(HomeModel())
                isAssignableFrom(DirectoryViewModel::class.java) ->
                    DirectoryViewModel(DirectoryModel())
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}