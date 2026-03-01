package com.erman.usurf.application.di

import android.content.Context
import com.erman.usurf.activity.MainViewModel
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.data.ApplicationPreferenceProvider
import com.erman.usurf.application.data.ApplicationPreferencesRepositoryImpl
import com.erman.usurf.application.domain.ApplicationPreferencesRepository
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.data.FtpRepositoryImpl
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FTPLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.ui.FTPViewModel
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.home.data.FavoriteRepositoryImpl
import com.erman.usurf.home.data.HomePreferenceProvider
import com.erman.usurf.home.data.HomePreferencesRepositoryImpl
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.domain.HomePreferencesRepository
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.preference.data.PreferencesRepositoryImpl
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.preference.ui.PreferencesViewModel
import com.erman.usurf.storage.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.storage.data.StorageDirectoryRepositoryImpl
import com.erman.usurf.storage.data.StoragePathsProviderImpl
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.SHARED_PREF_FILE
import io.realm.Realm
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single { ApplicationPreferenceProvider(get()) }
        single<ApplicationPreferencesRepository> { ApplicationPreferencesRepositoryImpl(get()) }
        single<StoragePathsProvider> { StoragePathsProviderImpl(get(), get()) }
        single { ApplicationDao(get(), get()) }
        viewModel { MainViewModel(get(), get()) }

        single { PreferenceProvider(get()) }
        single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
        single { StorageDirectoryPreferenceProvider(get()) }
        single<StorageDirectoryRepository> { StorageDirectoryRepositoryImpl(get()) }
        single { DirectoryModel(get(), get(), get(), get(), get()) }
        viewModel { DirectoryViewModel(get(), get()) }

        single { HomeModel(get()) }
        single { HomePreferenceProvider(get(), get()) }
        single<HomePreferencesRepository> { HomePreferencesRepositoryImpl(get()) }
        single<FavoriteRepository> { FavoriteRepositoryImpl(get(), get()) }
        viewModel { HomeViewModel(get(), get(), get(), get(), get()) }

        single { FtpModel(get()) }
        single { ConnectionLiveData(get()) }
        single { FTPLiveData(get()) }
        single<FtpRepository> { FtpRepositoryImpl(get(), get(), get(), get(), get()) }
        viewModel { FTPViewModel(get()) }

        single {
            val context: Context = get()
            return@single context.getSharedPreferences(
                SHARED_PREF_FILE,
                Context.MODE_PRIVATE,
            )
        }

        single {
            return@single Realm.getDefaultInstance()
        }

        single { FtpPreferenceProvider(get(), get()) }
        single { RootHandler() }
        single { FavoriteDao(get()) }
        viewModel { PreferencesViewModel(get(), get()) }
    }
