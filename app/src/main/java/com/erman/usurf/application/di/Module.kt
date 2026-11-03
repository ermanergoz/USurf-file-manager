package com.erman.usurf.application.di

import android.content.Context
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.erman.usurf.activity.MainViewModel
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.data.ApplicationPreferenceProvider
import com.erman.usurf.application.data.ApplicationPreferencesRepositoryImpl
import com.erman.usurf.application.domain.ApplicationPreferencesRepository
import com.erman.usurf.dialog.ui.FileInformationViewModel
import com.erman.usurf.directory.domain.ArchiveRepository
import com.erman.usurf.directory.domain.DirectoryRepository
import com.erman.usurf.directory.domain.FolderSizeRepository
import com.erman.usurf.directory.model.ArchiveOperations
import com.erman.usurf.directory.model.DirectoryListHandler
import com.erman.usurf.directory.model.DirectoryModel
import com.erman.usurf.directory.model.FileCompressionHandler
import com.erman.usurf.directory.model.FileCopier
import com.erman.usurf.directory.model.FileCopierImpl
import com.erman.usurf.directory.model.P7ZipApi
import com.erman.usurf.directory.model.RootHandler
import com.erman.usurf.directory.model.RootWriter
import com.erman.usurf.directory.model.SevenZipExecutor
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.data.FtpPreferenceProvider
import com.erman.usurf.ftp.data.FtpRepositoryImpl
import com.erman.usurf.ftp.data.FtpServerState
import com.erman.usurf.ftp.data.FtpSettingsRepositoryImpl
import com.erman.usurf.ftp.domain.FtpCredentialsInitializer
import com.erman.usurf.ftp.domain.FtpRepository
import com.erman.usurf.ftp.domain.FtpSettingsRepository
import com.erman.usurf.ftp.model.ConnectionLiveData
import com.erman.usurf.ftp.model.FtpModel
import com.erman.usurf.ftp.service.FtpServiceLauncher
import com.erman.usurf.ftp.ui.FTPViewModel
import com.erman.usurf.home.data.FavoriteDao
import com.erman.usurf.home.data.FavoriteRepositoryImpl
import com.erman.usurf.home.data.RealmFavoriteDao
import com.erman.usurf.home.data.favoritesRealm
import com.erman.usurf.home.domain.FavoriteRepository
import com.erman.usurf.home.model.HomeModel
import com.erman.usurf.home.ui.HomeViewModel
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.preference.data.PreferencesRepositoryImpl
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.preference.domain.RootAccessChecker
import com.erman.usurf.preference.ui.PreferencesViewModel
import com.erman.usurf.storage.data.SafDocumentAccess
import com.erman.usurf.storage.data.SafDocumentAccessImpl
import com.erman.usurf.storage.data.StorageAccessPolicyImpl
import com.erman.usurf.storage.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.storage.data.StorageDirectoryRepositoryImpl
import com.erman.usurf.storage.data.StoragePathsProviderImpl
import com.erman.usurf.storage.data.StorageVolumeProbe
import com.erman.usurf.storage.domain.StorageAccessPolicy
import com.erman.usurf.storage.domain.StorageDirectoriesSource
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import com.erman.usurf.utils.SHARED_PREF_FILE
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val IO_DISPATCHER: String = "io"

val appModule =
    module {
        single<CoroutineDispatcher>(named(IO_DISPATCHER)) { Dispatchers.IO }

        single { ApplicationPreferenceProvider(get()) }
        single<ApplicationPreferencesRepository> { ApplicationPreferencesRepositoryImpl(get()) }
        single {
            StoragePathsProviderImpl(
                probeVolumes = StorageVolumeProbe(get())::probe,
                preferencesRepository = get(),
                rootAccessPreference = get<PreferenceProvider>().observeRootAccessPreference(),
                ioDispatcher = get(named(IO_DISPATCHER))
            )
        }
        single<StoragePathsProvider> { get<StoragePathsProviderImpl>() }
        single<StorageDirectoriesSource> { get<StoragePathsProviderImpl>() }
        single { ApplicationDao(get(), get(), get(named(IO_DISPATCHER))) }
        viewModel { MainViewModel(get(), get(), get(), get()) }

        single { PreferenceProvider(get()) }
        single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
        single { StorageDirectoryPreferenceProvider(get()) }
        single<StorageDirectoryRepository> { StorageDirectoryRepositoryImpl(get()) }
        single<SafDocumentAccess> {
            val context: Context = get()
            SafDocumentAccessImpl(get(), get()) { treeUri -> DocumentFile.fromTreeUri(context, treeUri) }
        }
        single<StorageAccessPolicy> {
            val context: Context = get()
            StorageAccessPolicyImpl(
                storagePathsProvider = get(),
                storageDirectoryRepository = get(),
                sdkInt = Build.VERSION.SDK_INT,
                isRemovableVolume = StorageAccessPolicyImpl::isRemovableStorageVolume,
                isGrantHeld = { treeUri -> StorageAccessPolicyImpl.holdsWriteGrantOn(context, treeUri) },
                grantedTreeOf = StorageAccessPolicyImpl::grantedTreeOf
            )
        }
        single { DirectoryListHandler(get(), get(), get(named(IO_DISPATCHER))) }
        single<FileCopier> {
            val context: Context = get()
            FileCopierImpl(get()) { uri -> context.contentResolver.openOutputStream(uri) }
        }
        single { RootWriter(get(), get(), get(), Build.VERSION.SDK_INT, get<Context>().packageName) }
        single { DirectoryModel(get(), get(), get(), get(), get(), get(named(IO_DISPATCHER))) }
        single<DirectoryRepository> { get<DirectoryModel>() }
        single<ArchiveRepository> { ArchiveOperations(get(), get<DirectoryModel>(), get(), get(named(IO_DISPATCHER))) }
        // The lambda defers loading the native library to the first 7-Zip run.
        single { SevenZipExecutor { arguments -> P7ZipApi.executeCommand(arguments) } }
        single {
            val context: Context = get()
            FileCompressionHandler(get(), context.cacheDir)
        }
        viewModel { DirectoryViewModel(get(), get(), get(), get(), get(), get()) }
        single<FolderSizeRepository> { get<DirectoryModel>() }
        viewModel { FileInformationViewModel(get()) }

        single { HomeModel() }
        single { favoritesRealm().build() }
        single<FavoriteDao> { RealmFavoriteDao(get()) }
        single<FavoriteRepository> { FavoriteRepositoryImpl(get(), get(), get(named(IO_DISPATCHER))) }
        viewModel { HomeViewModel(get(), get(), get(), get(), get(named(IO_DISPATCHER))) }

        single { FtpModel() }
        single { FtpServiceLauncher(get()) }
        single { ConnectionLiveData(get()) }
        single { FtpServerState() }
        single<FtpRepository> { FtpRepositoryImpl(get(), get(), get(), get(), get()) }
        single<FtpSettingsRepository> { FtpSettingsRepositoryImpl(get()) }
        viewModel { FTPViewModel(get(), get(), get(named(IO_DISPATCHER))) }

        single {
            val context: Context = get()
            return@single context.getSharedPreferences(
                SHARED_PREF_FILE,
                Context.MODE_PRIVATE
            )
        }

        single { FtpPreferenceProvider(get(), get(), FtpCredentialsInitializer(SecureRandom())) }
        single { RootHandler() }
        single<RootAccessChecker> { get<RootHandler>() }
        viewModel { PreferencesViewModel(get(), get(), get(named(IO_DISPATCHER))) }
    }
