package com.erman.usurf.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erman.usurf.ftp.model.FTPHelper
import com.erman.usurf.ftp.ui.FTPFragmentViewModel

/*
We can not create ViewModel on our own. We need ViewModelProviders utility provided by
Android to create ViewModels.But ViewModelProviders can only instantiate ViewModels
with no arg constructor. So if we have a ViewModel with multiple arguments,
then we need to use a Factory that we can pass to ViewModelProviders to use when
an instance of MyViewModel is required.
 */

class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T /*T is our view model class*/ =
        with(modelClass) {
            when {
                isAssignableFrom(FTPFragmentViewModel::class.java) ->
                    FTPFragmentViewModel(FTPHelper())
                //TODO: Add rest of the view models
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}