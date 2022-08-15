package com.erman.usurf.application.di

import android.content.Context
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.ui.FTPViewModel
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.home.data.HomePreferenceProvider
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.utils.SHARED_PREF_FILE
import io.realm.Realm
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val AppModule = module {
    single { DirectoryModel(get(), get(), get()) }
    viewModel { DirectoryViewModel(get(), get()) }

    single { HomeModel() }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }

    single { FtpModel() }
    viewModel { FTPViewModel(get(), get()) }

    single {
        val context: Context = get()
        return@single context.getSharedPreferences(
            SHARED_PREF_FILE, Context.MODE_PRIVATE
        )
    }

    single {
        return@single Realm.getDefaultInstance()
    }

    single { PreferenceProvider(get()) }
    single { StorageDirectoryPreferenceProvider(get()) }
    single { FtpPreferenceProvider(get()) }
    single { RootHandler() }
    single { HomePreferenceProvider(get()) }
    single { FavoriteDao(get()) }
}
