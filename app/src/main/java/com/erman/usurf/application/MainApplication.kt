package com.erman.usurf.application

import android.app.Application
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.di.appModule
import com.erman.usurf.application.domain.ApplicationPreferencesRepository
import com.erman.usurf.directory.model.RootHandler
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MainApplication : Application() {
    private val applicationPreferencesRepository: ApplicationPreferencesRepository by inject()
    private val applicationDao: ApplicationDao by inject()

    // Work the process does for itself, outside any screen: it lasts as long as the process.
    private val applicationScope: CoroutineScope = MainScope()

    override fun onCreate() {
        super.onCreate()

        RootHandler.configureShell()
        Realm.init(this)
        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }

        if (applicationPreferencesRepository.getIsFirstLaunch()) {
            applicationScope.launch {
                applicationDao.addInitialFavorites()
                applicationPreferencesRepository.setIsFirstLaunch(false)
            }
        }
    }
}
