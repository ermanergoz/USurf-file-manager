package com.erman.usurf.application.di

import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.ui.FTPViewModel
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.ui.HomeViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module


val AppModule =  module {
    single { DirectoryModel() }
    viewModel { DirectoryViewModel(get()) }
    single { HomeModel() }
    viewModel { HomeViewModel(get()) }
    single { FtpModel() }
    viewModel { FTPViewModel(get()) }

    //single {
    //    val context: Context = get()
    //    return@single context.getSharedPreferences(
    //        SHARED_PREF_FILE, Context.MODE_PRIVATE
    //    )
    //}
}